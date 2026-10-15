package ru.net.avz.test.moex_alert.alerts.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.alerts.dto.AlertWebSocketDto;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertWebSocketHandlerTest {

    private AlertWebSocketHandler handler;

    private ObjectMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = spy(new ObjectMapper());
        handler = new AlertWebSocketHandler(new SimpleAsyncTaskExecutor(), jsonMapper);
    }

    @Test
    @DisplayName("Should have correct endpoint specification")
    void shouldHaveCorrectEndpointSpec() {
        assertThat(AlertWebSocketHandler.ENDPOINT.path()).isEqualTo("/ws/alerts");
        assertThat(AlertWebSocketHandler.ENDPOINT.messageType()).isEqualTo(AlertWebSocketDto.class);
    }

    @Nested
    @DisplayName("Connection Lifecycle")
    class ConnectionLifecycleTests {

        @Test
        @DisplayName("afterConnectionEstablished should store session and put lock attribute")
        void afterConnectionEstablished_storesSessionAndLock() {

            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-123");
            when(session.getAttributes()).thenReturn(attributes);

            handler.afterConnectionEstablished(session);

            assertThat(attributes).containsKey(AlertWebSocketHandler.ATTR_KEY_LOCK);
            assertThat(attributes.get(AlertWebSocketHandler.ATTR_KEY_LOCK)).isInstanceOf(ReentrantLock.class);
        }

        @Test
        @DisplayName("afterConnectionClosed should remove session and lock attribute")
        void afterConnectionClosed_removesSessionAndLock() {

            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-123");
            when(session.getAttributes()).thenReturn(attributes);

            handler.afterConnectionEstablished(session);
            assertThat(attributes).containsKey(AlertWebSocketHandler.ATTR_KEY_LOCK);
            assertThat(attributes.get(AlertWebSocketHandler.ATTR_KEY_LOCK)).isInstanceOf(ReentrantLock.class);

            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
            assertThat(attributes).doesNotContainKey(AlertWebSocketHandler.ATTR_KEY_LOCK);
        }
    }

    @Nested
    @DisplayName("broadcastAlert()")
    class BroadcastAlertTests {

        private static final Duration TIMEOUT = Duration.ofSeconds(5);
        private static final TickerEntity TICKER = TestHelper.asSaved(TestHelper.createTickerBuilder("SBER")).build();

        @Test
        @DisplayName("Should return false when no sessions are registered")
        void broadcastAlert_noSessions_returnsFalse() {

            AlertEntity alert = TestHelper.createAlertBuilder().build();

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);

            assertThat(resultFuture).isCompletedWithValue(false);
            verify(jsonMapper, never()).writeValueAsString(any());
        }

        @Test
        @DisplayName("Should return true when broadcast succeeds to open sessions")
        void broadcastAlert_successfulSend_returnsTrue() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-1");
            when(session.isOpen()).thenReturn(true);
            when(session.getAttributes()).thenReturn(attributes);
            handler.afterConnectionEstablished(session);

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertThat(result).isTrue();
            verify(jsonMapper, times(1)).writeValueAsString(any());
            verify(session, times(1)).sendMessage(any());
        }

        @Test
        @DisplayName("Should handle JSON serialization exception and return false")
        void broadcastAlert_serializationException_returnsFalse() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-1");
//            when(session.isOpen()).thenReturn(true);
            when(session.getAttributes()).thenReturn(attributes);
            doThrow(JacksonException.class).when(jsonMapper).writeValueAsString(any(AlertWebSocketDto.class));
            handler.afterConnectionEstablished(session);

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertThat(result).isFalse();
            verify(jsonMapper, times(1)).writeValueAsString(any());
            verify(session, never()).isOpen();
            verify(session, never()).sendMessage(any());
        }

        @Test
        @DisplayName("Should handle IOException during sendMessage and return false")
        void broadcastAlert_ioException_returnsFalse() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-1");
            when(session.isOpen()).thenReturn(true);
            when(session.getAttributes()).thenReturn(attributes);
            doThrow(IOException.class).when(session).sendMessage(any(TextMessage.class));
            handler.afterConnectionEstablished(session);

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertThat(result).isFalse();
            verify(jsonMapper, times(1)).writeValueAsString(any());
            verify(session, times(1)).sendMessage(any());
        }

        @Test
        @DisplayName("Should force close session and return false for unreliable session")
        void broadcastAlert_unreliableSession_closeSessionAndReturnsFalse() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-1");
            when(session.isOpen()).thenReturn(true);
            when(session.getAttributes()).thenReturn(attributes);
            doThrow(new CompletionException(new TimeoutException())).when(session).sendMessage(any(TextMessage.class));
            handler.afterConnectionEstablished(session);

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertThat(result).isFalse();
            verify(jsonMapper, times(1)).writeValueAsString(any());
            verify(session, times(1)).sendMessage(any());
            verify(session, times(1)).close(CloseStatus.SESSION_NOT_RELIABLE);
        }

        @Test
        @DisplayName("Should return true when one of session is closed")
        void broadcastAlert_closedSession_returnsTrue() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            Map<String, Object> attributes1 = new HashMap<>();
            Map<String, Object> attributes2 = new HashMap<>();

            when(session1.getId()).thenReturn("session-1");
            when(session1.isOpen()).thenReturn(false);
            when(session1.getAttributes()).thenReturn(attributes1);
            handler.afterConnectionEstablished(session1);
            when(session2.getId()).thenReturn("session-2");
            when(session2.isOpen()).thenReturn(true);
            when(session2.getAttributes()).thenReturn(attributes2);
            handler.afterConnectionEstablished(session2);

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertThat(result).isTrue();
            verify(jsonMapper, times(1)).writeValueAsString(any());
            verify(session1, never()).sendMessage(any());
            verify(session2, times(1)).sendMessage(any());
        }

        @Test
        @DisplayName("Should return false when one of session is failed")
        void broadcastAlert_failSend_returnsFalse() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            Map<String, Object> attributes1 = new HashMap<>();
            Map<String, Object> attributes2 = new HashMap<>();

            when(session1.getId()).thenReturn("session-1");
            when(session1.isOpen()).thenReturn(true);
            when(session1.getAttributes()).thenReturn(attributes1);
            doThrow(IOException.class).when(session1).sendMessage(any(TextMessage.class));
            handler.afterConnectionEstablished(session1);
            when(session2.getId()).thenReturn("session-2");
            when(session2.isOpen()).thenReturn(true);
            when(session2.getAttributes()).thenReturn(attributes2);
            handler.afterConnectionEstablished(session2);

            CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertThat(result).isFalse();
            verify(jsonMapper, times(1)).writeValueAsString(any());
            verify(session1, times(1)).sendMessage(any());
            verify(session2, times(1)).sendMessage(any());
        }

        @Test
        @DisplayName("Should handle lock timeout when lock cannot be acquired")
        void broadcastAlert_lockTimeout_returnsFalse() throws IOException {

            AlertEntity alert = TestHelper.asSaved(TestHelper.createAlertBuilder(TICKER)).build();
            WebSocketSession session = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();

            when(session.getId()).thenReturn("session-1");
            when(session.isOpen()).thenReturn(true);
            when(session.getAttributes()).thenReturn(attributes);
            handler.afterConnectionEstablished(session);

            ReentrantLock lock = (ReentrantLock) attributes.get(AlertWebSocketHandler.ATTR_KEY_LOCK);
            lock.lock();
            try {

                CompletableFuture<Boolean> resultFuture = handler.broadcastAlert(alert, Duration.ofMillis(500));
                assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                Boolean result = resultFuture.resultNow();

                assertThat(result).isFalse();
                verify(jsonMapper, times(1)).writeValueAsString(any());
                verify(session, never()).sendMessage(any(TextMessage.class));

            } finally {
                lock.unlock();
            }
        }
    }
}
