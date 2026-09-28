package ru.net.avz.test.moex_alert.alerts.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.UUID;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.prices.fields.PriceAmountField;
import ru.net.avz.test.moex_alert.prices.fields.PriceCurrencyField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AlertWebSocketDto(

        @NotNull
        @UUID
        java.util.UUID id,

        @NotNull
        @TickerNameField
        String ticker,

        @NotNull
        LocalDateTime ts,

        @NotNull
        @PriceAmountField
        BigDecimal amount,

        @NotNull
        @PriceCurrencyField
        String currency

) {
    public static AlertWebSocketDto of(AlertEntity alert) {
        return AlertWebSocketDto.builder()
                .id(alert.getId())
                .ticker(alert.getTicker().getName())
                .ts(alert.getTs())
                .amount(alert.getAmount())
                .currency(alert.getCurrency())
                .build();
    }
}
