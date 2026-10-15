package ru.net.avz.test.moex_alert;

import org.junit.platform.commons.annotation.Contract;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.common.AbstractEntity;
import ru.net.avz.test.moex_alert.prices.PriceEntity;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TestHelper {

    String DEFAULT_CURRENCY = "RUB";

    String TICKER_DEFAULT_NAME = "SBER";
    Float TICKER_DEFAULT_THRESHOLD = 0.12f;
    String TICKER_DEFAULT_CURRENCY = DEFAULT_CURRENCY;

    static TickerEntity.TickerEntityBuilder<?, ?> createTickerBuilder() {
        return createTickerBuilder(TICKER_DEFAULT_NAME);
    }

    static TickerEntity.TickerEntityBuilder<?, ?> createTickerBuilder(String name) {
        return TickerEntity.builder()
                .name(name)
                .threshold(TICKER_DEFAULT_THRESHOLD)
                .currency(TICKER_DEFAULT_CURRENCY);
    }

    LocalDateTime PRICE_DEFAULT_TS = LocalDateTime.of(2025, 1, 1, 12, 0);
    BigDecimal PRICE_DEFAULT_AMOUNT = new BigDecimal("123.45");
    String PRICE_DEFAULT_CURRENCY = TICKER_DEFAULT_CURRENCY;

    static PriceEntity.PriceEntityBuilder<?, ?> createPriceBuilder() {
        return createPriceBuilder(createTickerBuilder().build());
    }

    static PriceEntity.PriceEntityBuilder<?, ?> createPriceBuilder(TickerEntity ticker) {
        return PriceEntity.builder()
                .ticker(ticker)
                .ts(PRICE_DEFAULT_TS)
                .amount(PRICE_DEFAULT_AMOUNT)
                .currency(PRICE_DEFAULT_CURRENCY);
    }

    LocalDateTime ALERT_DEFAULT_TS = LocalDateTime.of(2026, 1, 1, 12, 0);
    BigDecimal ALERT_DEFAULT_AMOUNT = new BigDecimal("321.54");
    String ALERT_DEFAULT_CURRENCY = PRICE_DEFAULT_CURRENCY;
    Float ALERT_DEFAULT_DIFF = 12.3f;

    static AlertEntity.AlertEntityBuilder<?, ?> createAlertBuilder() {
        return createAlertBuilder(createTickerBuilder().build());
    }

    static AlertEntity.AlertEntityBuilder<?, ?> createAlertBuilder(TickerEntity ticker) {
        return AlertEntity.builder()
                .ticker(ticker)
                .ts(ALERT_DEFAULT_TS)
                .amount(ALERT_DEFAULT_AMOUNT)
                .currency(ALERT_DEFAULT_CURRENCY)
                .diff(ALERT_DEFAULT_DIFF)
                .sendAttempts(0)
                .nextSendAfter(null)
                .sentAt(null);
    }

    static <C extends AbstractEntity, B extends AbstractEntity.AbstractEntityBuilder<C, ?>> B asSaved(
            B builder) {

        builder.id(UUID.randomUUID());
        builder.createdAt(ALERT_DEFAULT_TS);
        builder.updatedAt(ALERT_DEFAULT_TS);
        return builder;
    }

    Answer<?> repositorySaveStub = invocation -> {
        return invocation.getArgument(0);
    };

    Answer<?> transactionExecuteStub = invocation -> {
        TransactionCallback<?> callback = invocation.getArgument(0);
        return callback.doInTransaction(null);
    };
}
