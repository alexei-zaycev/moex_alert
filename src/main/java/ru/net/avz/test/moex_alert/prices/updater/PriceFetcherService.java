package ru.net.avz.test.moex_alert.prices.updater;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.net.avz.test.moex_alert.prices.PriceEntity;
import ru.net.avz.test.moex_alert.prices.PriceRepository;
import ru.net.avz.test.moex_alert.prices.exceptions.PriceFetchException;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.TickerService;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceFetcherService
        implements PriceFetcher {

    private static final Duration CONN_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private final PriceRepository priceRepository;
    private final TickerService tickerService;
    private final TransactionTemplate tx;

    @Override
    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
    public List<PriceEntity> loadAndSavePrices()
            throws PriceFetchException {

        return loadAndSavePrices(DEFAULT_READ_TIMEOUT);
    }

    @Override
    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
    public List<PriceEntity> loadAndSavePrices(
            Duration timeout)
            throws PriceFetchException {

        List<TickerEntity> tickers = tickerService.findAll();
        if (tickers.isEmpty()) return List.of();

        List<PriceEntity> prices = fetchPricesFromMoex(tickers, timeout);
        if (prices.isEmpty()) return List.of();

        // заливаем в базу цены
        // ручное управление транзакцией чтоб не выносить код в отдельный класс (а в этот класс - будет мисс мимо прокси)
        return tx.execute(_ -> {

            // защиты от удаления тикеров с момента начала метода loadAndSavePrices
            Set<UUID> ids =
                    tickerService.findAll()
                            .stream()
                            .map(TickerEntity::getId)
                            .collect(Collectors.toSet());

            return priceRepository.saveAll(
                    prices.stream()
                            .filter(price -> ids.contains(price.getTicker().getId()))
                            .toList());
        });
    }

    // TODO сделать rate limit
    protected List<PriceEntity> fetchPricesFromMoex(
            List<TickerEntity> tickers,
            Duration timeout)
            throws PriceFetchException {

        if (tickers.isEmpty()) return List.of();
        try {

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(CONN_TIMEOUT);
            requestFactory.setReadTimeout(timeout);

            String xml = RestClient.builder()
                    .requestFactory(requestFactory)
                    .build()
                    .get()
                    .uri("https://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities.xml?iss.meta=off&iss.only=marketdata&marketdata.columns=SECID,LAST,LCURRENTPRICE")
                    .retrieve()
                    .body(String.class);

            return this._parseMoexXml(xml, tickers);

        } catch (RestClientException |
                 XMLStreamException |
                 NumberFormatException ex) {

            throw new PriceFetchException(ex);
        }
    }

    protected List<PriceEntity> _parseMoexXml(
            String xml,
            List<TickerEntity> tickers)
            throws XMLStreamException, NumberFormatException {

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        String currency = "RUB";

        try (StringReader stringReader = new StringReader(xml)) {
            XMLStreamReader reader = factory.createXMLStreamReader(stringReader);
            try {

                Map<String, TickerEntity> tickersByName =
                        tickers.stream()
                                .filter(ticker -> ticker.getCurrency().equals(currency))
                                .collect(Collectors.toMap(TickerEntity::getName, Function.identity()));

                List<PriceEntity> prices =
                        new ArrayList<>(tickers.size());

                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT && "row".equals(reader.getLocalName())) {
                        String rowSecId = null;
                        String rowLast = null;
//                        String rowLCurrentPrice = null;
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            switch (reader.getAttributeName(i).toString()) {
                                case "SECID":
                                    rowSecId = reader.getAttributeValue(i);
                                    break;
                                case "LAST":
                                    rowLast = reader.getAttributeValue(i);
                                    break;
//                                case "LCURRENTPRICE":
//                                    rowLCurrentPrice = reader.getAttributeValue(i);
//                                    break;
                            }
                        }
                        if (rowSecId != null && rowLast != null && !rowLast.isBlank()) {
                            TickerEntity ticker = tickersByName.get(rowSecId);
                            //noinspection StatementWithEmptyBody
                            if (ticker != null) {
                                prices.add(
                                        PriceEntity.builder()
                                                .ticker(ticker)
                                                .ts(LocalDateTime.now())
                                                .amount(new BigDecimal(rowLast))
                                                .currency(currency)
                                                .build());
                            } else {
                                // IGNORE неизвестный тикер
                            }
                        }
                    }
                }

                log.info("Parsed {} prices from MOEX for registered {} tickers", prices.size(), tickersByName.size());

                return prices;

            } finally {
                reader.close();
            }
        }
    }
}
