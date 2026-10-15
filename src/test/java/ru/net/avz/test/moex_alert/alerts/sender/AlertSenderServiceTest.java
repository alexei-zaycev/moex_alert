package ru.net.avz.test.moex_alert.alerts.sender;

import org.apache.commons.lang3.function.TriConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;
import ru.net.avz.test.moex_alert.alerts.AlertRepository;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertSenderServiceTest {

    private AlertSenderService alertSenderService;

    private AlertRepository alertRepository;
    private AlertWebSocketHandler alertWebSocketHandler;
    private TransactionTemplate tx;

    @Captor private ArgumentCaptor<UUID> idCaptor;
    @Captor private ArgumentCaptor<LocalDateTime> dateTimeCaptor;
    @Captor private ArgumentCaptor<Integer> attemptsCaptor;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        alertWebSocketHandler = mock(AlertWebSocketHandler.class);
        tx = mock(TransactionTemplate.class);
        alertSenderService = new AlertSenderService(alertRepository, alertWebSocketHandler, tx);
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final TickerEntity TICKER = TestHelper.asSaved(TestHelper.createTickerBuilder("SBER")).build();
    private static final LocalDateTime TS_PAST = LocalDateTime.now().minusDays(1);
    private static final LocalDateTime TS_FUTURE = LocalDateTime.now().plusDays(1);

    @Nested
    @DisplayName("sendAlert() - early exit")
    class SendAlertEarlyExitTests {

        private void assertEarlyExit(
                AlertEntity alert,
                CompletableFuture<Boolean> actualSendAlertResult) {

            assertThat(actualSendAlertResult).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = actualSendAlertResult.resultNow();

            assertThat(result).isFalse();
            verify(alertWebSocketHandler, never()).broadcastAlert(any());
            verify(alertRepository, never()).markAlertAsSent(any(), any());
            verify(alertRepository, never()).markAlertAsNotSent(any(), anyInt(), any());
        }

        @Test
        @DisplayName("Should return false when alert.sentAt is already set")
        void sendAlert_alreadySent_returnsFalse() {

            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(TS_PAST)
                            .sendAttempts(1)
            ).build();

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);

            assertEarlyExit(alert, resultFuture);
        }

        @Test
        @DisplayName("Should return false when alert.nextSendAfter is null")
        void sendAlert_noNextSendAfter_returnsFalse() {

            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .nextSendAfter(null)
            ).build();

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);

            assertEarlyExit(alert, resultFuture);
        }

        @Test
        @DisplayName("Should return false when both sentAt is set and nextSendAfter is null")
        void sendAlert_bothConditionsMet_returnsFalse() {

            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(TS_PAST)
                            .nextSendAfter(null)
            ).build();

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);

            assertEarlyExit(alert, resultFuture);
        }
    }

    @Nested
    @DisplayName("sendAlert() - successful broadcast")
    class SendAlertSuccessTests {

        private void assertMarkAlertAsSent(
                AlertEntity alert,
                Boolean actualSendAlertResult,
                BiConsumer<UUID, LocalDateTime> markAlertAsSent) {

            assertThat(actualSendAlertResult).isTrue();
            verify(alertWebSocketHandler, times(1)).broadcastAlert(alert);
            verify(tx, times(1)).executeWithoutResult(any());
            verify(alertRepository, times(1)).markAlertAsSent(idCaptor.capture(), dateTimeCaptor.capture());
            verify(alertRepository, never()).markAlertAsNotSent(any(), anyInt(), any());

            markAlertAsSent.accept(
                    idCaptor.getValue(),
                    dateTimeCaptor.getValue());
        }

        @Test
        @DisplayName("Should mark alert as sent when broadcast succeeds")
        void sendAlert_broadcastSucceeds_marksAlertAsSent() {

            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(null)
                            .nextSendAfter(TS_PAST)
                            .sendAttempts(1)
            ).build();

            when(alertWebSocketHandler.broadcastAlert(alert)).thenReturn(CompletableFuture.completedFuture(true));
            doAnswer(TestHelper.transactionExecuteWithoutResultStub).when(tx).executeWithoutResult(any());

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertMarkAlertAsSent(alert, result, (actualId, actualSentAt) -> {
                assertThat(actualId).isEqualTo(alert.getId());
                assertThat(actualSentAt).isNotNull();
            });
        }

        @Test
        @DisplayName("Should execute transaction only after broadcast completes successfully")
        void sendAlert_broadcastSucceeds_transactionExecutedAfterBroadcast() throws InterruptedException {

            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(null)
                            .nextSendAfter(TS_PAST)
                            .sendAttempts(1)
            ).build();

            CompletableFuture<Boolean> broadcastFuture = new CompletableFuture<>();
            when(alertWebSocketHandler.broadcastAlert(alert)).thenReturn(broadcastFuture);
            doAnswer(TestHelper.transactionExecuteWithoutResultStub).when(tx).executeWithoutResult(any());

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);
            Thread.sleep(Duration.ofSeconds(1).toMillis());
            assertThat(resultFuture).isNotDone();

            broadcastFuture.complete(true);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertMarkAlertAsSent(alert, result, (actualId, actualSentAt) -> {
                assertThat(actualId).isEqualTo(alert.getId());
                assertThat(actualSentAt).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("sendAlert() - failed broadcast")
    class SendAlertFailureTests {

        private void assertMarkAlertAsNotSent(
                AlertEntity alert,
                Boolean actualSendAlertResult,
                TriConsumer<UUID, Integer, LocalDateTime> assertMarkAlertAsNotSent) {

            assertThat(actualSendAlertResult).isFalse();
            verify(alertWebSocketHandler, times(1)).broadcastAlert(alert);
            verify(tx, times(1)).executeWithoutResult(any());
            verify(alertRepository, never()).markAlertAsSent(any(), any());
            verify(alertRepository, times(1)).markAlertAsNotSent(idCaptor.capture(), attemptsCaptor.capture(), dateTimeCaptor.capture());

            assertMarkAlertAsNotSent.accept(
                    idCaptor.getValue(),
                    attemptsCaptor.getValue(),
                    dateTimeCaptor.getValue());
        }

        @Test
        @DisplayName("Should mark alert as not sent and returns false when broadcast fails")
        void sendAlert_broadcastFails_marksAlertAsNotSent_returnsFalse() {

            int sendAttempts = 2;
            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(null)
                            .nextSendAfter(TS_PAST)
                            .sendAttempts(sendAttempts)
            ).build();

            when(alertWebSocketHandler.broadcastAlert(alert)).thenReturn(CompletableFuture.completedFuture(false));
            doAnswer(TestHelper.transactionExecuteWithoutResultStub).when(tx).executeWithoutResult(any());

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertMarkAlertAsNotSent(alert, result, (actualId, actualAttempts, actualNextSendAfter) -> {
                assertThat(actualId).isEqualTo(alert.getId());
                assertThat(actualAttempts).isEqualTo(sendAttempts);
                assertThat(actualNextSendAfter).isNotNull();
            });
        }

        @Test
        @DisplayName("Should mark alert as not sent and returns false when broadcast throws")
        void sendAlert_broadcastThrows_marksAlertAsNotSent_returnsFalse() {

            int sendAttempts = 2;
            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(null)
                            .nextSendAfter(TS_PAST)
                            .sendAttempts(sendAttempts)
            ).build();

            when(alertWebSocketHandler.broadcastAlert(alert)).thenReturn(CompletableFuture.failedFuture(new IOException("WebSocket error")));
            doAnswer(TestHelper.transactionExecuteWithoutResultStub).when(tx).executeWithoutResult(any());

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertMarkAlertAsNotSent(alert, result, (actualId, actualAttempts, actualNextSendAfter) -> {
                assertThat(actualId).isEqualTo(alert.getId());
                assertThat(actualAttempts).isEqualTo(sendAttempts);
                assertThat(actualNextSendAfter).isNotNull();
            });
        }

        @Test
        @DisplayName("Should allow retry after low sendAttempts")
        void sendAlert_allowRetry_onLowAttempts() {

            int sendAttempts = 5;
            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(null)
                            .nextSendAfter(TS_PAST)
                            .sendAttempts(sendAttempts)
            ).build();

            when(alertWebSocketHandler.broadcastAlert(alert)).thenReturn(CompletableFuture.completedFuture(false));
            doAnswer(TestHelper.transactionExecuteWithoutResultStub).when(tx).executeWithoutResult(any());

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertMarkAlertAsNotSent(alert, result, (actualId, actualAttempts, actualNextSendAfter) -> {
                assertThat(actualId).isEqualTo(alert.getId());
                assertThat(actualAttempts).isEqualTo(sendAttempts);
                assertThat(actualNextSendAfter).isNotNull();
            });
        }

        @Test
        @DisplayName("Should stop retry after high sendAttempts")
        void sendAlert_stopRetry_onHighAttempts() {

            int sendAttempts = 9999;
            AlertEntity alert = TestHelper.asSaved(
                    TestHelper.createAlertBuilder(TICKER)
                            .sentAt(null)
                            .nextSendAfter(TS_PAST)
                            .sendAttempts(sendAttempts)
            ).build();

            when(alertWebSocketHandler.broadcastAlert(alert)).thenReturn(CompletableFuture.completedFuture(false));
            doAnswer(TestHelper.transactionExecuteWithoutResultStub).when(tx).executeWithoutResult(any());

            CompletableFuture<Boolean> resultFuture = alertSenderService.sendAlert(alert);
            assertThat(resultFuture).succeedsWithin(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Boolean result = resultFuture.resultNow();

            assertMarkAlertAsNotSent(alert, result, (actualId, actualAttempts, actualNextSendAfter) -> {
                assertThat(actualId).isEqualTo(alert.getId());
                assertThat(actualAttempts).isEqualTo(sendAttempts);
                assertThat(actualNextSendAfter).isNull();
            });
        }
    }
}
