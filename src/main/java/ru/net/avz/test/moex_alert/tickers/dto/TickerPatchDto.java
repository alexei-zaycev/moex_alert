package ru.net.avz.test.moex_alert.tickers.dto;

import jakarta.annotation.Nullable;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerThresholdField;

public record TickerPatchDto(

        @Nullable
        @TickerNameField
        String name,

        @Nullable
        @TickerThresholdField
        Float threshold

){

    public TickerEntity.TickerEntityBuilder applyTo(TickerEntity ticker) {
        var builder = ticker.toBuilder();
        if (this.name != null) builder.name(this.name);
        if (this.threshold != null) builder.threshold(this.threshold);
        return builder;
    }
}
