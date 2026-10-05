package ru.net.avz.test.moex_alert.alerts.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.alerts.AlertRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertSenderService
        implements AlertSender {

    private final AlertRepository alertRepository;
    private final AlertWebSocketHandler alertWebSocketHandler;
    private final TransactionTemplate tx;

    @Override
    public CompletableFuture<Boolean> sendAlert(
            AlertEntity alert) {

        if (alert.getSentAt() != null || alert.getNextSendAfter() == null)
            return CompletableFuture.completedFuture(false);

        return alertWebSocketHandler.broadcastAlert(alert).handle((isSent, ex) -> {
            if (ex == null) {
                // оборачиваем в транзакцию
                // защита от гонки встроена в логику запроса markAlertAsSent/markAlertAsNotSent
                tx.executeWithoutResult(_ -> {
                    if (isSent) {
                        alertRepository.markAlertAsSent(
                                alert.getId(),
                                LocalDateTime.now());
                    } else {
                        alertRepository.markAlertAsNotSent(
                                alert.getId(),
                                alert.getSendAttempts(),
                                _generateNextSendAfter(alert.getSendAttempts()).orElse(null));
                    }
                });
                return isSent;
            } else {
                log.error("Error sending alert {}", alert.getId(), ex);
                return false;
            }
        });
    }

    protected Optional<LocalDateTime> _generateNextSendAfter(
            int attempts) {

        if (attempts < 5) {
            return Optional.of(LocalDateTime.now().plusMinutes(1));
        } else if (attempts < 10) {
            return Optional.of(LocalDateTime.now().plusMinutes(5));
        } else if (attempts < 20) {
            return Optional.of(LocalDateTime.now().plusMinutes(10));
        } else {
            return Optional.empty();
        }
    }
}
