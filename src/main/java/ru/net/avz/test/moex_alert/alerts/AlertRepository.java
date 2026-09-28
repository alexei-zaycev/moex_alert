package ru.net.avz.test.moex_alert.alerts;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

interface AlertSignalRaw {

    UUID getTickerId();

    String getCurrency();

    /** если NULL - то значит это технический якорь */
    @Nullable BigDecimal getAlertAmountLast();

    BigDecimal getAlertAmountNew();
}

interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

    @Query(value = """
        WITH PRICES_STATS AS (SELECT P.ticker_id,
                                     count(*)                                               as price_cnt,
                                     percentile_disc(0.05) WITHIN GROUP (ORDER BY P.amount) as price_lower,
                                     percentile_disc(0.50) WITHIN GROUP (ORDER BY P.amount) as price_median,
                                     percentile_disc(0.95) WITHIN GROUP (ORDER BY P.amount) as price_upper
                              FROM prices P
                              INNER JOIN tickers T ON T.id = P.ticker_id AND T.currency = P.currency
                              WHERE P.ts > NOW() - INTERVAL '1 HOURS'
                              GROUP BY P.ticker_id),
             LAST_ALERTS AS (SELECT DISTINCT ON (A.ticker_id) A.ticker_id,
                                                              A.amount
                             FROM alerts A
                             INNER JOIN tickers T ON T.id = A.ticker_id AND T.currency = A.currency
                             ORDER BY A.ticker_id DESC, A.ts DESC, A.id DESC),
             NEW_SIGNALS AS (SELECT P.ticker_id,
                                    T.currency,
                                    CASE WHEN A.amount IS NULL THEN 'set'
                                         WHEN 100.0 * (P.price_upper - A.amount) / A.amount > T.threshold OR
                                              100.0 * (A.amount - P.price_lower) / A.amount > T.threshold THEN 'move' END as alert_type,
                                    A.amount as alert_amount_last,
                                    P.price_median as alert_amount_new,
                                    P.price_lower,
                                    P.price_median,
                                    P.price_upper
                             FROM tickers T
                             INNER JOIN PRICES_STATS P ON P.ticker_id = T.id
                             LEFT JOIN LAST_ALERTS A ON A.ticker_id = T.id
                             WHERE A.amount IS NULL OR P.price_cnt >= 10)
        SELECT ticker_id,
               currency,
               alert_amount_last,
               alert_amount_new
        FROM NEW_SIGNALS
        WHERE alert_type IS NOT NULL
          AND alert_amount_new > 0
          AND ticker_id IN (:ids)
    """, nativeQuery = true)
    List<AlertSignalRaw> detectAlertSignals(
            @Param("ids") List<UUID> tickers
    );

    @Query("""
        SELECT a
        FROM AlertEntity a
        INNER JOIN FETCH a.ticker t
        WHERE a.sentAt IS NULL
          AND a.nextSendAfter IS NOT NULL
          AND a.nextSendAfter <= :ts
        ORDER BY a.ts, t.name
    """)
    List<AlertEntity> findReadyForResendAlerts(
            @Param("ts") LocalDateTime ts
    );

    @Transactional
    @Modifying
    @Query("""
        UPDATE AlertEntity a
        SET a.sendAttempts = a.sendAttempts + 1,
            a.nextSendAfter = null,
            a.sentAt = :sentAt
        WHERE a.id = :id
          AND a.sentAt IS NULL
    """)
    void markAlertAsSent(
            @Param("id") UUID  id,
            @Param("sentAt") LocalDateTime sentAt
    );

    @Transactional
    @Modifying
    @Query("""
        UPDATE AlertEntity a
        SET a.sendAttempts = a.sendAttempts + 1,
            a.nextSendAfter = :nextSendAfter
        WHERE a.id = :id
          AND a.sentAt IS NULL
          AND a.sendAttempts = :sendAttempts
    """)
    void markAlertAsNotSent(
            @Param("id") UUID  id,
            @Param("sendAttempts") int currentSendAttempts,
            @Param("nextSendAfter") @Nullable LocalDateTime nextSendAfter
    );
}
