package ru.net.avz.test.moex_alert.alerts.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.UUID;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.prices.fields.PriceAmountField;
import ru.net.avz.test.moex_alert.prices.fields.PriceCurrencyField;
import ru.net.avz.test.moex_alert.tickers.dto.TickerDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Builder
public record AlertDto(

        @NotNull
        @UUID
        java.util.UUID id,

        @NotNull
        TickerDto ticker,

        @NotNull
        LocalDateTime ts,

        @NotNull
        @PriceAmountField
        BigDecimal amount,

        @NotNull
        @PriceCurrencyField
        String currency

//        @NotNull
//        @AlertSendAttemptsField
//        Integer sendAttempts,
//
//        @Nullable
//        LocalDateTime nextSendAfter,
//
//        @Nullable
//        LocalDateTime sentAt,

) {
    public static AlertDto of(AlertEntity alert) {
        return AlertDto.builder()
                .id(alert.getId())
                .ticker(TickerDto.of(alert.getTicker()))
                .ts(alert.getTs())
                .amount(alert.getAmount())
                .currency(alert.getCurrency())
//                .sendAttempts(alert.getSendAttempts())
//                .nextSendAfter(alert.getNextSendAfter())
//                .sentAt(alert.getSentAt())
                .build();
    }
}
