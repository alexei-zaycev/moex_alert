package ru.net.avz.test.moex_alert.alerts;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ru.net.avz.test.moex_alert.alerts.exceptions.AlertNotFoundException;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.TickerService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private static final int RESEND_PRICES_RATE_MINUTES = 1;

    private final AlertRepository alertRepository;
    private final TickerService tickerService;
    private final AlertWebSocketHandler alertWebSocketHandler;

    public Page<AlertEntity> findAll(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    public AlertEntity findByIdOrThrow(
            UUID alertId
    ) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    private static final AtomicBoolean _runningScheduleResendAlerts = new AtomicBoolean(false);

    @Scheduled(fixedRate = RESEND_PRICES_RATE_MINUTES, timeUnit = TimeUnit.MINUTES)
    protected void scheduleResendAlerts() {
        if (_runningScheduleResendAlerts.compareAndSet(false, true)) {
            LocalDateTime start = LocalDateTime.now();
            log.debug("🚀 Alerts resend job started");
            Mono.fromCallable(() -> alertRepository.findReadyForResendAlerts(LocalDateTime.now()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(this::trySendAlert)
                    .collectList()
                    .doOnSuccess(flags -> log.debug("✅ Alerts resend job completed, processed {} (duration: {} ms)",
                            flags != null ? flags.stream().filter(isSent -> isSent).count() : 0,
                            Duration.between(start, LocalDateTime.now()).toMillis()))
                    .doOnError(ex -> log.error("❌ Alerts resend job failed (duration: {} ms)",
                            Duration.between(start, LocalDateTime.now()).toMillis(),
                            ex))
                    .doFinally(signal -> _runningScheduleResendAlerts.set(false))
                    .subscribe();
        }
    }

    public Mono<List<AlertEntity>> detectAndSaveAlerts(
            int priceMinCount
    ) {
        return Mono.fromCallable(tickerService::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tickers -> detectAndSaveAlerts(tickers, priceMinCount));
    }

    private static final AtomicBoolean _runningDetectAndSaveAlerts = new AtomicBoolean(false);

    public Mono<List<AlertEntity>> detectAndSaveAlerts(
            List<TickerEntity> tickers,
            int priceMinCount
    ) {
        if (tickers.isEmpty()) {
            return Mono.empty();
        }
        if (_runningDetectAndSaveAlerts.compareAndSet(false, true)) {
            return Mono.fromCallable(() -> alertRepository.detectAlertSignals(tickers.stream().map(TickerEntity::getId).toList(), priceMinCount))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(signals -> !signals.isEmpty() ? Mono.just(signals) : Mono.empty())
                    .flatMapMany(Flux::fromIterable)
                    .map(signal -> Tuples.of(signal,
                            AlertEntity.builder()
                                    .ticker(tickers.stream().filter(ticker -> ticker.getId().equals(signal.getTickerId())).findAny().orElseThrow())
                                    .ts(LocalDateTime.now())
                                    .amount(signal.getAlertAmountNew())
                                    .diff(signal.getAlertAmountLast() == null ? null : Math.max(0f, Math.min(100f,
                                            BigDecimal.valueOf(100.00).multiply(
                                                    signal.getPriceUpper().subtract(signal.getAlertAmountLast()).max(
                                                    signal.getAlertAmountLast().subtract(signal.getPriceLower()))
                                            ).divide(signal.getAlertAmountLast(), 2, RoundingMode.CEILING).floatValue())))
                                    .currency(signal.getCurrency())
                                    .sendAttempts(0)
                                    .nextSendAfter(signal.getAlertAmountLast() == null
                                            ? null
                                            : LocalDateTime.now())
                                    .sentAt(signal.getAlertAmountLast() == null
                                            ? LocalDateTime.now()
                                            : null)
                                    .build()))
                    .collectList()
                    .map(t -> {
                        List<AlertEntity> alerts = alertRepository.saveAll(t.stream().map(Tuple2::getT2).toList());
                        alerts.forEach(alert -> {
                            t.stream().map(Tuple2::getT1).filter(s -> s.getTickerId().equals(alert.getTicker().getId())).findAny().ifPresent(signal ->
                                    log.info("New {}: id={}, ticker={}, amount={}->{}[{};{}], diff={}, currency={}",
                                            signal.getAlertAmountLast() == null
                                                    ? "ANCHOR"
                                                    : "SIGNAL",
                                            alert.getId(),
                                            alert.getTicker().getName(),
                                            signal.getAlertAmountLast(),
                                            alert.getAmount(),
                                            signal.getPriceLower(),
                                            signal.getPriceUpper(),
                                            alert.getDiff() != null
                                                    ? String.format(Locale.ENGLISH, "%.2f%%", alert.getDiff())
                                                    : null,
                                            alert.getCurrency()));
                        });
                        return alerts;
                    })
                    .flatMapMany(Flux::fromIterable)
                    .filter(alert -> alert.getSentAt() == null && alert.getNextSendAfter() != null)
                    .flatMap(alert -> this.trySendAlert(alert).thenReturn(alert), 1)
                    .collectList()
                    .doFinally(signal -> _runningDetectAndSaveAlerts.set(false));
        } else {
            return Mono.empty();
        }
    }

    protected Mono<Boolean> trySendAlert(
            AlertEntity alert
    ) {
        if (alert.getSentAt() != null || alert.getNextSendAfter() == null) {
            return Mono.empty();
        }
        return _sendAlert(alert)
                .publishOn(Schedulers.boundedElastic())
                .map(isSent -> {
                    if (isSent) {
                        alertRepository.markAlertAsSent(
                                alert.getId(),
                                LocalDateTime.now());
                    } else {
                        alertRepository.markAlertAsNotSent(
                                alert.getId(),
                                alert.getSendAttempts(),
                                _generateNextSendAfter(alert.getSendAttempts()));
                    }
                    return isSent;
                })
                .onErrorResume(ex -> {
                    log.error("Error sending alert {}", alert.getId(), ex);
                    return Mono.just(false);
                });
    }

    private @Nullable LocalDateTime _generateNextSendAfter(
            int attempts
    ) {
        if (attempts < 5) {
            return LocalDateTime.now().plusMinutes(1);
        } else if (attempts < 10) {
            return LocalDateTime.now().plusMinutes(5);
        } else if (attempts < 20) {
            return LocalDateTime.now().plusMinutes(10);
        } else {
            return null;
        }
    }

    private Mono<Boolean> _sendAlert(
            AlertEntity alert
    ) {
        return Mono.fromCallable(() -> alertWebSocketHandler.broadcastAlert(alert))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
