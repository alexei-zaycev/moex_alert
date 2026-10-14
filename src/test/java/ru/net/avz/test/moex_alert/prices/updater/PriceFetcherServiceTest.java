package ru.net.avz.test.moex_alert.prices.updater;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.prices.PriceEntity;
import ru.net.avz.test.moex_alert.prices.PriceRepository;
import ru.net.avz.test.moex_alert.prices.exceptions.PriceFetchException;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.TickerService;

import javax.xml.stream.XMLStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceFetcherServiceTest {

    private PriceFetcherService priceFetcherService;

    private PriceRepository priceRepository;
    private TickerService tickerService;
    private TransactionTemplate tx;
    @Captor private ArgumentCaptor<List<PriceEntity>> pricesCaptor;

    @BeforeEach
    void setUp() {
        priceRepository = mock(PriceRepository.class);
        tickerService = mock(TickerService.class);
        tx = mock(TransactionTemplate.class);
        priceFetcherService = new PriceFetcherService(priceRepository, tickerService, tx);
    }

    private static final String VALID_XML = """
        <document>
        <data id="marketdata">
        <rows>
        <row SECID="GAZC" LAST="" LCURRENTPRICE="659"/>
        <row SECID="GAZP" LAST="105.48" LCURRENTPRICE="105.15"/>
        <row SECID="SBDS" LAST="3.375" LCURRENTPRICE="3.385"/>
        <row SECID="SBER" LAST="286.21" LCURRENTPRICE="286.17"/>
        <row SECID="SBERP" LAST="286.59" LCURRENTPRICE="286.49"/>
        <row SECID="SBFR" LAST="15.69" LCURRENTPRICE="15.69"/>
        </rows>
        </data>
        </document>
    """;

    private static final TickerEntity VALID_TICKER_1 =
            TestHelper.createTickerBuilder()
                    .id(UUID.randomUUID())
                    .name("SBER")
                    .build();

    private static final TickerEntity VALID_TICKER_2 =
            TestHelper.createTickerBuilder()
                    .id(UUID.randomUUID())
                    .name("GAZP")
                    .build();

    private static final PriceEntity VALID_PRICE_1 =
            TestHelper.createPriceBuilder(VALID_TICKER_1)
                    .amount(new BigDecimal("286.21"))
                    .currency("RUB")
                    .build();

    private static final PriceEntity VALID_PRICE_2 =
            TestHelper.createPriceBuilder(VALID_TICKER_2)
                    .amount(new BigDecimal("105.48"))
                    .currency("RUB")
                    .build();

    private final Comparator<PriceEntity> priceComparator =
            Comparator.<PriceEntity, String>comparing(price -> price.getTicker().getName())
                    .thenComparing(PriceEntity::getAmount)
                    .thenComparing(PriceEntity::getCurrency);

    private static final String MALFORMED_XML = """
        <?xml version=\"1.0\"?><response><rows><row SECID=\"SBER\"
    """;

    private static final String INVALID_LAST_XML = """
        <document>
        <data id="marketdata">
        <rows>
        <row SECID="SBER" LAST="286,21"/>
        </rows>
        </data>
        </document>
    """;

    @Nested
    @DisplayName("loadPrices()")
    class loadPrices {

        @Test
        @DisplayName("loadPrices should return empty when tickers list is empty")
        void loadPricesShouldReturnEmptyWhenTickersIsEmpty() {
            List<PriceEntity> result = priceFetcherService.loadPrices(List.of(), Duration.ofSeconds(30));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw PriceFetchException wrapping XMLStreamException from _parseMoexXml")
        void shouldThrowPriceFetchExceptionWrappingXmlStreamException() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            PriceFetcherService spy = spy(priceFetcherService);
            doReturn(MALFORMED_XML).when(spy)._fetchPricesFromMoex(any(Duration.class));
            assertThatThrownBy(() -> spy.loadPrices(tickers, Duration.ofSeconds(30)))
                    .isInstanceOf(PriceFetchException.class)
                    .hasCauseInstanceOf(XMLStreamException.class);
        }

        @Test
        @DisplayName("Should throw PriceFetchException wrapping NumberFormatException from _parseMoexXml")
        void shouldThrowPriceFetchExceptionWrappingNumberFormatException() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            PriceFetcherService spy = spy(priceFetcherService);
            doReturn(INVALID_LAST_XML).when(spy)._fetchPricesFromMoex(any(Duration.class));
            assertThatThrownBy(() -> spy.loadPrices(tickers, Duration.ofSeconds(30)))
                    .isInstanceOf(PriceFetchException.class)
                    .hasCauseInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("parseMoexXml()")
    class ParseMoexXmlTests {

        private static final String EMPTY_ROWS_XML = """
            <document>
            <data id="marketdata">
            <rows>
            </rows>
            </data>
            </document>
        """;

        private static final String EMPTY_LAST_XML = """
            <document>
            <data id="marketdata">
            <rows>
            <row SECID="SBER" LAST="286.21"/>
            <row SECID="GAZP" LAST=""/>
            </rows>
            </data>
            </document>
        """;

        private static final String BLANK_LAST_XML = """
            <document>
            <data id="marketdata">
            <rows>
            <row SECID="SBER" LAST="286.21"/>
            <row SECID="GAZP" LAST="   "/>
            </rows>
            </data>
            </document>
        """;

        private static final String MISSING_LAST_XML = """
            <document>
            <data id="marketdata">
            <rows>
            <row SECID="SBER" LAST="286.21"/>
            <row SECID="GAZP"/>
            </rows>
            </data>
            </document>
        """;

        private static final String DUPLICATE_SECID_XML = """
            <document>
            <data id="marketdata">
            <rows>
            <row SECID="SBER" LAST="286.21"/>
            <row SECID="SBER" LAST="280.21"/>
            </rows>
            </data>
            </document>
        """;

        @Test
        @DisplayName("Should throw XMLStreamException on malformed XML")
        void shouldThrowXmlStreamExceptionOnMalformedXml() {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            assertThatThrownBy(() -> priceFetcherService._parseMoexXml(MALFORMED_XML, tickers))
                    .isInstanceOf(XMLStreamException.class);
        }

        @Test
        @DisplayName("Should return empty list for XML with no rows")
        void shouldReturnEmptyListForXmlWithNoRows() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            List<PriceEntity> result = priceFetcherService._parseMoexXml(EMPTY_ROWS_XML, tickers);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should ignore rows with unknown SECID")
        void shouldIgnoreUnknownSecurities() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1);
            List<PriceEntity> result = priceFetcherService._parseMoexXml(VALID_XML, tickers);
            assertThat(result)
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);
        }

        @Test
        @DisplayName("Should skip rows with empty LAST values")
        void shouldSkipRowsWithEmptyLastValues() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            List<PriceEntity> result = priceFetcherService._parseMoexXml(EMPTY_LAST_XML, tickers);
            assertThat(result)
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);
        }

        @Test
        @DisplayName("Should skip rows with blank LAST values")
        void shouldSkipRowsWithBlankLastValues() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            List<PriceEntity> result = priceFetcherService._parseMoexXml(BLANK_LAST_XML, tickers);
            assertThat(result)
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);
        }

        @Test
        @DisplayName("Should skip rows with missing LAST values")
        void shouldSkipRowsWithMissingLastValues() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            List<PriceEntity> result = priceFetcherService._parseMoexXml(MISSING_LAST_XML, tickers);
            assertThat(result)
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);
        }


        @Test
        @DisplayName("Should use first occurrence for duplicate SECID in XML")
        void shouldUseFirstOccurrenceForDuplicateSecid() throws Exception {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            List<PriceEntity> result = priceFetcherService._parseMoexXml(DUPLICATE_SECID_XML, tickers);
            assertThat(result)
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);
        }


        @Test
        @DisplayName("Should throw NumberFormatException for invalid LAST values")
        void shouldThrowNumberFormatExceptionOnInvalidLastValues() {
            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);
            assertThatThrownBy(() -> priceFetcherService._parseMoexXml(INVALID_LAST_XML, tickers))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Should return empty when all tickers are non-RUB")
        void shouldReturnEmptyWhenAllTickersAreNonRub() throws Exception {
            List<TickerEntity> tickers = List.of(
                    TestHelper.createTickerBuilder().name(VALID_TICKER_1.getName()).currency("USD").build(),
                    TestHelper.createTickerBuilder().name(VALID_TICKER_2.getName()).currency("EUR").build());
            List<PriceEntity> result = priceFetcherService._parseMoexXml(VALID_XML, tickers);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("loadAndSavePrices()")
    class loadAndSavePrices {

        private final Answer<?> priceSaveAllStub = invocation -> invocation.getArgument(0);

        private final Answer<?> txStub = invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        };

        @Test
        @DisplayName("Should return empty list when no tickers exist")
        void shouldReturnEmptyListWhenNoTickersExist() {

            when(tickerService.findAll()).thenReturn(List.of());

            List<PriceEntity> result = priceFetcherService.loadAndSavePrices();
            assertThat(result).isEmpty();

            verify(priceRepository, Mockito.never()).saveAll(any());
            verify(tx, Mockito.never()).execute(any());
        }

        @Test
        @DisplayName("Should return empty when tickers exist but none match XML")
        void shouldReturnEmptyWhenTickersExistButNoneMatchXml() {

            TickerEntity xyz = TestHelper.createTickerBuilder().id(UUID.randomUUID()).name("XYZ").build();
            List<TickerEntity> tickers = List.of(xyz);

            PriceFetcherService spy = spy(priceFetcherService);
            when(tickerService.findAll()).thenReturn(tickers);
            doReturn(VALID_XML).when(spy)._fetchPricesFromMoex(any(Duration.class));

            List<PriceEntity> result = spy.loadAndSavePrices();
            assertThat(result).isEmpty();

            verify(priceRepository, Mockito.never()).saveAll(any());
            verify(tx, Mockito.never()).execute(any());
        }

        @Test
        @DisplayName("Should fetch, parse, and save all prices on happy path")
        void shouldFetchParseAndSaveAllPricesOnHappyPath() {

            List<TickerEntity> tickers = List.of(VALID_TICKER_1, VALID_TICKER_2);

            PriceFetcherService spy = spy(priceFetcherService);
            when(tickerService.findAll()).thenReturn(tickers);
            when(priceRepository.saveAll(any())).thenAnswer(priceSaveAllStub);
            when(tx.execute(any())).thenAnswer(txStub);
            doReturn(VALID_XML).when(spy)._fetchPricesFromMoex(any(Duration.class));

            List<PriceEntity> result = spy.loadAndSavePrices();
            assertThat(result)
                    .hasSize(2)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1, VALID_PRICE_2);

            verify(priceRepository, Mockito.times(1)).saveAll(pricesCaptor.capture());
            verify(tx, Mockito.times(1)).execute(any());
            assertThat(pricesCaptor.getValue())
                    .hasSize(2)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1, VALID_PRICE_2);
        }

        @Test
        @DisplayName("Should filter out prices for tickers deleted during load")
        void shouldFilterOutPricesForDeletedTickers() {

            List<TickerEntity> initialTickers = List.of(VALID_TICKER_1, VALID_TICKER_2);

            PriceFetcherService spy = spy(priceFetcherService);
            lenient().when(tickerService.findAll())
                    .thenReturn(initialTickers)
                    .thenReturn(List.of(VALID_TICKER_1));
            when(priceRepository.saveAll(any())).thenAnswer(priceSaveAllStub);
            when(tx.execute(any())).thenAnswer(txStub);
            doReturn(VALID_XML).when(spy)._fetchPricesFromMoex(any(Duration.class));

            List<PriceEntity> result = spy.loadAndSavePrices();
            assertThat(result)
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);

            verify(priceRepository, times(1)).saveAll(pricesCaptor.capture());
            verify(tx, Mockito.times(1)).execute(any());
            assertThat(pricesCaptor.getValue())
                    .hasSize(1)
                    .usingElementComparator(priceComparator)
                    .containsExactlyInAnyOrder(VALID_PRICE_1);
        }
    }
}
