package ru.net.avz.test.moex_alert.alerts;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.TickerService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private static final int RESEND_PRICES_RATE_MS = 60_000;

    private final AlertRepository alertRepository;
    private final TickerService tickerService;
    private final AlertWebSocketHandler alertWebSocketHandler;

    private static final AtomicBoolean _runningScheduleResendAlerts = new AtomicBoolean(false);

    @Scheduled(fixedRate = RESEND_PRICES_RATE_MS)
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

    public Mono<List<AlertEntity>> detectAndSaveAlerts() {
        return Mono.fromCallable(tickerService::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::detectAndSaveAlerts);
    }

    private static final AtomicBoolean _runningDetectAndSaveAlerts = new AtomicBoolean(false);

    public Mono<List<AlertEntity>> detectAndSaveAlerts(
            List<TickerEntity> tickers
    ) {
        if (tickers.isEmpty()) {
            return Mono.empty();
        }
        if (_runningDetectAndSaveAlerts.compareAndSet(false, true)) {
            return Mono.fromCallable(() -> alertRepository.detectAlertSignals(tickers.stream().map(TickerEntity::getId).toList()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(signals -> !signals.isEmpty() ? Mono.just(signals) : Mono.empty())
                    .flatMapMany(Flux::fromIterable)
                    .map(signal ->
                            AlertEntity.builder()
                                    .ticker(tickers.stream().filter(ticker -> ticker.getId().equals(signal.getTickerId())).findAny().orElseThrow())
                                    .ts(LocalDateTime.now())
                                    .amount(signal.getAlertAmountNew())
                                    .currency(signal.getCurrency())
                                    .sendAttempts(0)
                                    .nextSendAfter(signal.getAlertAmountLast() == null ? null : LocalDateTime.now())
                                    .sentAt(signal.getAlertAmountLast() == null ? LocalDateTime.now() : null)
                                    .build())
                    .collectList()
                    .map(alertRepository::saveAll)
                    .flatMapMany(Flux::fromIterable)
                    .filter(alert -> alert.getSentAt() == null && alert.getNextSendAfter() != null)
                    .flatMap(alert -> this.trySendAlert(alert).thenReturn(alert))
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
                    alert.setSendAttempts(alert.getSendAttempts() + 1);
                    if (isSent) {
                        alert.setNextSendAfter(null);
                        alert.setSentAt(LocalDateTime.now());
                    } else {
                        alert.setNextSendAfter(_generateNextSendAfter(alert.getSendAttempts()));
                    }
                    // для простоты мы допускаем, что оповещение может отправиться (_sendAlert),
                    // а данные в базу не будут внесены (save) из-за гонки
                    alertRepository.saveAndFlush(alert);
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
