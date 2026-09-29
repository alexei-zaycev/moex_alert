package ru.net.avz.test.moex_alert.alerts;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * делаем классический async для демонстрации стыка reactive-async.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertSenderService {

    private final AlertRepository alertRepository;
    private final AlertWebSocketHandler alertWebSocketHandler;

    @Async("alertsTaskExecutor")
    public CompletableFuture<Boolean> trySendAlertAsync(
            AlertEntity alert
    ) {
        if (alert.getSentAt() != null || alert.getNextSendAfter() == null) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            boolean isSent = alertWebSocketHandler.broadcastAlert(alert);
            setAlertSentState(alert, isSent);
            return CompletableFuture.completedFuture(isSent);
        } catch (Exception ex) {
            log.error("Error sending alert {}", alert.getId(), ex);
            return CompletableFuture.completedFuture(false);
        }
    }

    public Mono<Boolean> trySendAlertReactive(
            AlertEntity alert
    ) {
        if (alert.getSentAt() != null || alert.getNextSendAfter() == null) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> alertWebSocketHandler.broadcastAlert(alert))
                .subscribeOn(Schedulers.boundedElastic())
                .map(isSent -> {
                    setAlertSentState(alert, isSent);
                    return isSent;
                })
                .onErrorResume(ex -> {
                    log.error("Error sending alert {}", alert.getId(), ex);
                    return Mono.just(false);
                });
    }

    protected void setAlertSentState(
            AlertEntity alert,
            boolean isSent
    ) {
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
    }

    protected @Nullable LocalDateTime _generateNextSendAfter(
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
}
