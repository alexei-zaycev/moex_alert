package ru.net.avz.test.moex_alert.prices.dto;

import jakarta.validation.constraints.NotNull;
import ru.net.avz.test.moex_alert.common.dto.AbstractDto;
import ru.net.avz.test.moex_alert.common.fields.SumAmountField;
import ru.net.avz.test.moex_alert.common.fields.SumCurrencyField;
import ru.net.avz.test.moex_alert.prices.PriceEntity;
import ru.net.avz.test.moex_alert.tickers.dto.TickerDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceDto
        extends AbstractDto {

    @NotNull
    public final TickerDto ticker;

    @NotNull
    public final LocalDateTime ts;

    @NotNull
    @SumAmountField
    public final BigDecimal amount;

    @NotNull
    @SumCurrencyField
    public final String currency;

    public PriceDto(PriceEntity price) {
        super(price);
        this.ticker = new TickerDto(price.getTicker());
        this.ts = price.getTs();
        this.amount = price.getAmount();
        this.currency = price.getCurrency();
    }
}
