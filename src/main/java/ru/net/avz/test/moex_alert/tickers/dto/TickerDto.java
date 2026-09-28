package ru.net.avz.test.moex_alert.tickers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.UUID;
import ru.net.avz.test.moex_alert.prices.fields.PriceCurrencyField;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerThresholdField;

@Builder
public record TickerDto(

        @NotNull
        @UUID
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
