package ru.net.avz.test.moex_alert.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.net.avz.test.moex_alert.alerts.AlertService;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.TickerService;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    private static final int RELOAD_PRICES_RATE_MINUTES = 10;
    //    private static final int RELOAD_PRICES_RATE_MINUTES = 1;
    private static final int SCAN_PRICES_MIN_COUNT = 3;             // ~ 60 / RELOAD_PRICES_RATE_MINUTES / 2
    private static final int RELOAD_PRICES_TIMEOUT_SECONDS = 30;

    private final PriceRepository priceRepository;
    private final TickerService tickerService;
    private final AlertService alertService;
    private final WebClient webClient;

    private static final AtomicBoolean _runningScheduleLoadAndSavePrices = new AtomicBoolean(false);

    @Scheduled(fixedRate = RELOAD_PRICES_RATE_MINUTES, timeUnit = TimeUnit.MINUTES)
    protected void scheduleLoadAndSavePrices() {
        if (_runningScheduleLoadAndSavePrices.compareAndSet(false, true)) {
            LocalDateTime start = LocalDateTime.now();
            log.debug("🚀 Price refresh job started");
            loadAndSavePrices()
                    .doOnSuccess(prices -> log.debug("✅ Price refresh job completed, processed {} (duration: {} ms)",
                            prices != null ? prices.size() : 0,
                            Duration.between(start, LocalDateTime.now()).toMillis()))
                    .doOnError(ex -> log.error("❌ Price refresh job failed (duration: {} ms)",
                            Duration.between(start, LocalDateTime.now()).toMillis(),
                            ex))
                    .doFinally(signal -> _runningScheduleLoadAndSavePrices.set(false))
                    .subscribe();
        }
    }

    private static final AtomicBoolean _runningLoadAndSavePrices = new AtomicBoolean(false);

    public Mono<List<PriceEntity>> loadAndSavePrices() {
        if (_runningLoadAndSavePrices.compareAndSet(false, true)) {
            return Mono.fromCallable(tickerService::findAll)
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(this::fetchPricesFromMoex)
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(prices -> !prices.isEmpty() ? Mono.just(prices) : Mono.empty())
                    .map(priceRepository::saveAll)
                    .flatMap(prices -> {
                        List<TickerEntity> tickers = prices.stream().map(PriceEntity::getTicker).toList();
                        return alertService.detectAndSaveAlerts(tickers, SCAN_PRICES_MIN_COUNT).thenReturn(prices);
                    })
                    .doFinally(signal -> _runningLoadAndSavePrices.set(false));
        } else {
            return Mono.empty();
        }
    }

    public Mono<List<PriceEntity>> fetchPricesFromMoex(
            List<TickerEntity> tickers
    ) {
        if (tickers.isEmpty()) {
            return Mono.empty();
        }
        return webClient.get()
                .uri("http://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities.xml?iss.meta=off&iss.only=marketdata&marketdata.columns=SECID,LAST,LCURRENTPRICE")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(RELOAD_PRICES_TIMEOUT_SECONDS))
                .publishOn(Schedulers.boundedElastic())
                .map(xmlResponse -> _parseMoexXml(xmlResponse, tickers))
                .flatMap(prices -> prices.map(Mono::just).orElseGet(Mono::empty))
                .onErrorResume(ex -> {
                    log.error("Error querying MOEX", ex);
                    return Mono.empty();
                });
    }

    protected Optional<List<PriceEntity>> _parseMoexXml(
            String xml,
            List<TickerEntity> tickers
    ) {

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
                            if (ticker != null) {
                                try {
                                    prices.add(
                                            PriceEntity.builder()
                                                    .ticker(ticker)
                                                    .ts(LocalDateTime.now())
                                                    .amount(new BigDecimal(rowLast))
                                                    .currency(currency)
                                                    .build());
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid price value for {}: {}", rowSecId, rowLast);
                                }
                            }
                        }
                    }
                }

                log.info("Parsed {} prices from MOEX for registered {} tickers", prices.size(), tickersByName.size());

                return Optional.of(prices);

            } finally {
                reader.close();
            }
        } catch (Exception ex) {
            log.error("Error parsing MOEX response", ex);
            return Optional.empty();
        }
    }
}
