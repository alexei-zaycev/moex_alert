package ru.net.avz.test.moex_alert.alerts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.alerts.fields.AlertDiffField;
import ru.net.avz.test.moex_alert.common.dto.AbstractDto;
import ru.net.avz.test.moex_alert.common.fields.SumAmountField;
import ru.net.avz.test.moex_alert.common.fields.SumCurrencyField;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "AlertWebSocketDto")
public class AlertWebSocketDto
        extends AbstractDto {

    @NotNull
    @TickerNameField
    public final String ticker;

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

    public AlertWebSocketDto(AlertEntity alert) {
        super(alert);
        this.ticker = alert.getTicker().getName();
        this.ts = alert.getTs();
        this.amount = alert.getAmount();
        this.currency = alert.getCurrency();
        this.diff = alert.getDiff();
    }
}
