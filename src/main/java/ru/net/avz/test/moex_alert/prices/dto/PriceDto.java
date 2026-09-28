package ru.net.avz.test.moex_alert.prices.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.UUID;
import ru.net.avz.test.moex_alert.prices.PriceEntity;
import ru.net.avz.test.moex_alert.prices.fields.PriceAmountField;
import ru.net.avz.test.moex_alert.prices.fields.PriceCurrencyField;
import ru.net.avz.test.moex_alert.tickers.dto.TickerDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Builder
public record PriceDto(

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

) {
    public static PriceDto of(PriceEntity price) {
        return PriceDto.builder()
                .id(price.getId())
                .ticker(TickerDto.of(price.getTicker()))
                .ts(price.getTs())
                .amount(price.getAmount())
                .currency(price.getCurrency())
                .build();
    }
}
