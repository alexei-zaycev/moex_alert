package ru.net.avz.test.moex_alert.tickers.dto;

import jakarta.validation.constraints.NotNull;
import ru.net.avz.test.moex_alert.common.dto.AbstractDto;
import ru.net.avz.test.moex_alert.common.fields.SumCurrencyField;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerThresholdField;

public class TickerDto
        extends AbstractDto {

    @NotNull
    @TickerNameField
    public final String name;

    @NotNull
    @TickerThresholdField
    public final Float threshold;

    @NotNull
    @SumCurrencyField
    public final String currency;

    public TickerDto(TickerEntity ticker) {
        super(ticker);
        this.name = ticker.getName();
        this.threshold = ticker.getThreshold();
        this.currency = ticker.getCurrency();
    }
}
