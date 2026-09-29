package ru.net.avz.test.moex_alert.tickers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import ru.net.avz.test.moex_alert.common.fields.EntityIdField;
import ru.net.avz.test.moex_alert.prices.fields.PriceCurrencyField;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerThresholdField;

@Builder
public record TickerDto(

        @NotNull
        @EntityIdField
        java.util.UUID id,

        @NotNull
        @TickerNameField
        String name,

        @NotNull
        @TickerThresholdField
        Float threshold,

        @NotNull
        @PriceCurrencyField
        String currency

) {
    public static TickerDto of(TickerEntity ticker) {
        return TickerDto.builder()
                .id(ticker.getId())
                .name(ticker.getName())
                .threshold(ticker.getThreshold())
                .currency(ticker.getCurrency())
                .build();
    }
}
