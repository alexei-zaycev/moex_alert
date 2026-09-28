package ru.net.avz.test.moex_alert.alerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.net.avz.test.moex_alert.alerts.dto.AlertWebSocketDto;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final String _ATTR_KEY_LOCK = "lock";

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper jsonMapper = Json.mapper();

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session
    ) {
        session.getAttributes().put(AlertWebSocketHandler._ATTR_KEY_LOCK, new ReentrantLock());
        sessions.put(session.getId(), session);
        log.debug("WebSocket connection established. Active {} sessions", sessions.size());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            org.springframework.web.socket.CloseStatus status
    ) {
        session.getAttributes().remove(AlertWebSocketHandler._ATTR_KEY_LOCK);
        sessions.remove(session.getId());
        log.debug("WebSocket connection closed. Active {} sessions", sessions.size());
    }

    public boolean broadcastAlert(
            AlertEntity alert
    ) {

        // если сессий нет - считаем что ничего не доставили
        if (sessions.isEmpty()) {
            return false;
        }

        TextMessage message;
        try {

            message = new TextMessage(
                    jsonMapper.writeValueAsString(
                            AlertWebSocketDto.of(alert)));

        } catch (Exception ex) {
            log.error("Failed to serialize alert {}", alert.getId(), ex);
            return false;
        }


        int success = 0;
        for (WebSocketSession session : sessions.values()) {
            try {
                ReentrantLock lock = Objects.requireNonNull((ReentrantLock) session.getAttributes().get(AlertWebSocketHandler._ATTR_KEY_LOCK));
                if (lock.tryLock(10, TimeUnit.SECONDS)) {
                    try {
                        session.sendMessage(message);
                        success++;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.warn("Failed to send alert {} to session {}: lock timeout", alert.getId(), session.getId());
                }
            } catch (Exception ex) {
                log.warn("Failed to send alert {} to session {}", alert.getId(), session.getId(), ex);
            }
        }

        log.debug("Alert {} broadcasted to {} sessions", alert.getId(), success);

        return success > 0;
    }
}
