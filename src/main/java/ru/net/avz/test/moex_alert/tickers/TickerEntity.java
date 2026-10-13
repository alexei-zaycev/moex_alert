package ru.net.avz.test.moex_alert.tickers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.net.avz.test.moex_alert.common.AbstractEntity;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickers")
@Cache(region = "ticker-entity-cache", usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@EntityListeners(AuditingEntityListener.class)
public class TickerEntity
        extends AbstractEntity {

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
