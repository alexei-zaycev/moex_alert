package ru.net.avz.test.moex_alert.tickers;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickers")
@EntityListeners(AuditingEntityListener.class)
public class TickerEntity {

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

    @Column(length = 10, unique = true)
    @NotNull
    @Getter
    private String name;

    @Column
    @NotNull
    @Getter
    private Float threshold;

    @Column(length = 3)
    @NotNull
    @Getter
    private String currency;

    public static final String IDX_UNIQUE_NAME = "UQ__TICKERS__NAME";
}
