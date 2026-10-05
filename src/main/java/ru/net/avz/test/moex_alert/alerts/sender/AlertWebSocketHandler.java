package ru.net.avz.test.moex_alert.alerts.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.alerts.dto.AlertWebSocketDto;
import ru.net.avz.test.moex_alert.common.WebSocketEndpointSpec;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AlertWebSocketHandler
        extends TextWebSocketHandler {

    public static final WebSocketEndpointSpec ENDPOINT =
            WebSocketEndpointSpec.builder()
                    .path("/ws/alerts")
                    .operationTag("Alerts")
                    .operationId("wsAlerts")
                    .operationTitle("Подписка на алерты")
                    .messageType(AlertWebSocketDto.class)
                    .build();

    public static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);
    private static final String ATTR_KEY_LOCK = "lock";

    private final AsyncTaskExecutor taskExecutor;
    private final ObjectMapper jsonMapper;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public AlertWebSocketHandler(
            @Qualifier("alertsExecutor") AsyncTaskExecutor taskExecutor,
            ObjectMapper jsonMapper) {

        this.taskExecutor = taskExecutor;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) {

        session.getAttributes().put(ATTR_KEY_LOCK, new ReentrantLock());
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established. Active {} sessions", sessions.size());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            org.springframework.web.socket.CloseStatus status) {

        sessions.remove(session.getId());
        session.getAttributes().remove(ATTR_KEY_LOCK);
        log.info("WebSocket connection closed. Active {} sessions", sessions.size());
    }

    public CompletableFuture<Boolean> broadcastAlert(
            AlertEntity alert) {

        // если сессий нет - считаем что ничего не доставили
        if (sessions.isEmpty()) return CompletableFuture.completedFuture(false);

        return _buildMessage(alert)
                    .thenCompose(message -> _sendBroadcast(alert, message))
                    .thenApply(results -> {

                        long opened = results.size(),
                             success = results.stream().filter(r -> r).count();

                        log.info("Alert {} broadcasted to {} sessions of {}", alert.getId(), success, opened);

                        return success == opened;

                    }).exceptionally(ex -> {
                        log.warn("Failed to send alert {}", alert.getId(), ex);
                        return false;
                    });
    }

    protected CompletableFuture<TextMessage> _buildMessage(
            AlertEntity alert) {

        return CompletableFuture.<TextMessage>supplyAsync(() -> {

            return new TextMessage(
                    jsonMapper.writeValueAsString(
                            AlertWebSocketDto.of(alert)));

        });
    }

    protected CompletableFuture<List<Boolean>> _sendBroadcast(
            AlertEntity alert,
            TextMessage message) {

        List<CompletableFuture<Boolean>> tasks =
                sessions.values()
                        .stream()
                        .filter(WebSocketSession::isOpen)
                        .map(session -> _sendMessage(session, alert, message))
                        .toList();

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).thenApply(_ ->
                tasks.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    protected CompletableFuture<Boolean> _sendMessage(
            WebSocketSession session,
            AlertEntity alert,
            TextMessage message) {

        return CompletableFuture.<Boolean>supplyAsync(() -> {
                    if (session.isOpen()) {
                        ReentrantLock lock = Objects.requireNonNull((ReentrantLock) session.getAttributes().get(ATTR_KEY_LOCK));
                        try {
                            if (lock.tryLock(LOCK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                                try {
                                    session.sendMessage(message);
                                    return true;
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex); // обработаем централизованно в конце цепочки
                                } finally {
                                    lock.unlock();
                                }
                            } else {
                                log.warn("Failed to send alert {} to session {}: timeout", alert.getId(), session.getId());
                                return false;
                            }
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex); // обработаем централизованно в конце цепочки
                        }
                    } else {
                        return false;
                    }
                }, taskExecutor)
                .orTimeout((LOCK_TIMEOUT.toMillis() + SEND_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("Failed to send alert {} to session {}", alert.getId(), session.getId(), ex);
                    if (ex instanceof TimeoutException) {
                        try {
                            session.close(CloseStatus.SESSION_NOT_RELIABLE);
                        } catch (IOException _) {
                            // IGNORE
                        }
                    }
                    return false;
                });
    }
}
