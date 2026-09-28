package ru.net.avz.test.moex_alert.tickers.dto;

import jakarta.validation.constraints.NotNull;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerThresholdField;

public record TickerCreateDto(

        @NotNull
        @TickerNameField
        String name,

        @NotNull
        @TickerThresholdField
        Float threshold

){
    public TickerEntity.TickerEntityBuilder toBuilder() {
        return TickerEntity.builder()
                .name(this.name())
                .threshold(this.threshold())
                .currency("RUB");
    }
}
