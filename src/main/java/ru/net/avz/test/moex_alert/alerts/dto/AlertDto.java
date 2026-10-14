package ru.net.avz.test.moex_alert.alerts.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.alerts.fields.AlertDiffField;
import ru.net.avz.test.moex_alert.common.dto.AbstractDto;
import ru.net.avz.test.moex_alert.common.fields.SumAmountField;
import ru.net.avz.test.moex_alert.common.fields.SumCurrencyField;
import ru.net.avz.test.moex_alert.tickers.dto.TickerDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlertDto
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

    @Nullable
    @AlertDiffField
    public final Float diff;

    public AlertDto(AlertEntity alert) {
        super(alert);
        this.ticker = new TickerDto(alert.getTicker());
        this.ts = alert.getTs();
        this.amount = alert.getAmount();
        this.currency = alert.getCurrency();
        this.diff = alert.getDiff();
    }
}
