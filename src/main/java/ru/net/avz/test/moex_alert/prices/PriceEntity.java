package ru.net.avz.test.moex_alert.prices;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prices")
@EntityListeners(AuditingEntityListener.class)
public class PriceEntity {

    // <editor-fold defaultstate="collapsed" desc="id / createdAt / updatedAt">
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private UUID id;

    @CreatedDate
    @Column(updatable = false)
    @NotNull
    @Getter
    private LocalDateTime createdAt;

    @Version
    @LastModifiedDate
    @Column
    @NotNull
    @Getter
    private LocalDateTime updatedAt;
    // </editor-fold>

    @JoinColumn(name = "ticker_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull
    @Getter
    private TickerEntity ticker;

    @Column
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Getter
    private LocalDateTime ts;

    @Column(precision = 10, scale = 2)
    @NotNull
    @Getter
    private BigDecimal amount;

    @Column(length = 3)
    @NotNull
    @ColumnDefault("'RUB'")
    @Getter
    private String currency;
}
