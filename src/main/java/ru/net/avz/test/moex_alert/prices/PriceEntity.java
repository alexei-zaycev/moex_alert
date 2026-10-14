package ru.net.avz.test.moex_alert.prices;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.net.avz.test.moex_alert.common.AbstractEntity;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prices")
@EntityListeners(AuditingEntityListener.class)
public class PriceEntity
        extends AbstractEntity {

    @JoinColumn(name = "ticker_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull
    @Getter
    private TickerEntity ticker;

    @Column
    @NotNull
    @Getter
    private LocalDateTime ts;

    @Column(precision = 10, scale = 2)
    @NotNull
    @Getter
    private BigDecimal amount;

    @Column(length = 3)
    @NotNull
    @Getter
    private String currency;
}
