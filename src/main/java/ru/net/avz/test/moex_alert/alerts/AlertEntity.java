package ru.net.avz.test.moex_alert.alerts;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Table(name = "alerts")
@EntityListeners(AuditingEntityListener.class)
public class AlertEntity
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

    /** Вспомогательное поле для визуализации процентного движения */
    @Column
    @Nullable
    @Getter
    private Float diff;

    @Column
    @NotNull
    @Getter @Setter
    private Integer sendAttempts;

    @Column
    @Nullable
    @Getter @Setter
    private LocalDateTime nextSendAfter;

    @Column
    @Nullable
    @Getter @Setter
    private LocalDateTime sentAt;
}
