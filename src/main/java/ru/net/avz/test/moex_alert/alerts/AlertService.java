package ru.net.avz.test.moex_alert.alerts;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.net.avz.test.moex_alert.alerts.exceptions.AlertNotFoundException;
import ru.net.avz.test.moex_alert.alerts.sender.AlertSender;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.TickerService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    /** Периодичность попыток отослать не доставленные оповещения */
    private static final int RESEND_PRICES_RATE_MINUTES = 1;

    private final AlertRepository alertRepository;
    private final TickerService tickerService;
    private final AlertSender alertSender;
    private final TransactionTemplate tx;

    public Page<AlertEntity> findAll(
            Pageable pageable) {

        return alertRepository.findAll(pageable);
    }

    public AlertEntity findByIdOrThrow(
            UUID alertId) {

        return alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    private static final ReentrantLock _scheduleResendAlerts = new ReentrantLock();

    @Scheduled(fixedDelay = RESEND_PRICES_RATE_MINUTES, timeUnit = TimeUnit.MINUTES)
    protected void scheduleResendAlerts() {
        if (_scheduleResendAlerts.tryLock()) {
            LocalDateTime start = LocalDateTime.now();
            log.debug("🚀 Alerts resend job started");
            try {

                List<AlertEntity> alerts =
                        alertRepository.findReadyForResendAlerts(LocalDateTime.now());

                List<CompletableFuture<Boolean>> tasks =
                        alerts.stream()
                                .map(alertSender::sendAlert)
                                .toList();

                List<Boolean> results =
                        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                                .thenApply(_ -> tasks.stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.toList()))
                                .join();

                log.debug("✅ Alerts resend job completed, success {} of {} (duration: {} ms)",
                        results.stream().filter(r -> r).count(),
                        tasks.size(),
                        Duration.between(start, LocalDateTime.now()).toMillis());

            } catch (Exception ex) {
                log.error("❌ Alerts resend job failed (duration: {} ms)",
                        Duration.between(start, LocalDateTime.now()).toMillis(),
                        ex);
            } finally {
                _scheduleResendAlerts.unlock();
            }
        } else {
            log.warn("⌛ Alerts resend job rejected");
        }
    }

    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.BLOCK)
    public List<AlertEntity> detectAndSaveAlerts(
            int priceMinCount) {

        List<TickerEntity> tickers = tickerService.findAll();
        if (tickers.isEmpty()) return List.of();

        return detectAndSaveAlerts(tickers, priceMinCount);
    }

    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.BLOCK)
    public List<AlertEntity> detectAndSaveAlerts(
            List<TickerEntity> tickers,
            int priceMinCount) {

        if (tickers.isEmpty()) return List.of();

        List<UUID> tickerIds = tickers.stream().map(TickerEntity::getId).toList();
        List<AlertSignalRaw> signals = alertRepository.detectAlertSignals(tickerIds, priceMinCount);
        if (signals.isEmpty()) return List.of();

        Map<UUID, TickerEntity> tickersById =
                tickers.stream()
                        .collect(Collectors.toMap(TickerEntity::getId, Function.identity()));

        List<AlertEntity> alerts = new ArrayList<>(signals.size());
        for (AlertSignalRaw signal : signals) {

            TickerEntity ticker = Objects.requireNonNull(tickersById.get(signal.getTickerId()));

            alerts.add(
                    AlertEntity.builder()
                            .ticker(ticker)
                            .ts(LocalDateTime.now())
                            .amount(signal.getAlertAmountNew())
                            .diff(diffFromPreviousAlertAtPercents(signal))
                            .currency(signal.getCurrency())
                            .sendAttempts(0)
                            .nextSendAfter(signal.getAlertAmountLast() == null
                                    ? null
                                    : LocalDateTime.now())
                            .sentAt(signal.getAlertAmountLast() == null
                                    ? LocalDateTime.now()
                                    : null)
                            .build());
        }

        // заливаем в базу оповещения
        // ручное управление транзакцией чтоб не выносить код в отдельный класс (а в этот класс - будет мисс мимо прокси)
        List<AlertEntity> savedAlerts = tx.execute(_ -> {

            // защиты от удаления тикеров с момента начала метода detectAndSaveAlerts
            Set<UUID> ids =
                    tickerService.findAll()
                            .stream()
                            .map(TickerEntity::getId)
                            .collect(Collectors.toSet());

            return alertRepository.saveAll(
                    alerts.stream()
                            .filter(alert -> ids.contains(alert.getTicker().getId()))
                            .toList());
        });

        for (AlertEntity alert : savedAlerts) {

            if (log.isInfoEnabled()) {
                signals.stream()
                        .filter(signal -> signal.getTickerId().equals(alert.getTicker().getId()))
                        .findAny()
                        .ifPresent(signal -> {
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
                                    alert.getCurrency());
                        });
            }

            if (alert.getSentAt() == null && alert.getNextSendAfter() != null) {
                alertSender.sendAlert(alert);   // запустил-и-забыл
            }
        }

        return savedAlerts;
    }

    protected @Nullable Float diffFromPreviousAlertAtPercents(
            AlertSignalRaw signal) {

        if (signal.getAlertAmountLast() == null) return null;

        BigDecimal diffUp = signal.getPriceUpper().subtract(signal.getAlertAmountLast());
        BigDecimal diffDown = signal.getAlertAmountLast().subtract(signal.getPriceLower());
        int direction = diffUp.compareTo(diffDown) >= 0 ? 1 : -1;

        return BigDecimal.valueOf(100.00 * direction)
                .multiply(diffUp.max(diffDown))
                .divide(signal.getAlertAmountLast(), 2, RoundingMode.CEILING)
                .floatValue();
    }
}
