package ru.net.avz.test.moex_alert.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.net.avz.test.moex_alert.alerts.AlertService;
import ru.net.avz.test.moex_alert.prices.exceptions.PriceFetchException;
import ru.net.avz.test.moex_alert.prices.updater.PriceFetcher;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    /** Периодичность подгрузки данных с MOEX */
    private static final int RELOAD_PRICES_RATE_MINUTES = 10;

    /** Минимальное количество данных о ценах в окне, с которого начинается анализ сигналов */
    private static final int SCAN_PRICES_MIN_COUNT = 3;         // ~ 60 / RELOAD_PRICES_RATE_MINUTES / 2

    private final PriceRepository priceRepository;
    private final AlertService alertService;
    private final PriceFetcher priceFetcher;

    private static final ReentrantLock _scheduleLoadAndSavePrices = new ReentrantLock();

    @Scheduled(fixedDelay = RELOAD_PRICES_RATE_MINUTES, timeUnit = TimeUnit.MINUTES)
    protected void scheduleLoadAndSavePrices() {
        if (_scheduleLoadAndSavePrices.tryLock()) {
            LocalDateTime start = LocalDateTime.now();
            log.debug("🚀 Price refresh job started");
            try {

                List<PriceEntity> prices = loadAndSavePrices();

                log.debug("✅ Price refresh job completed, processed {} (duration: {} ms)",
                        prices.size(),
                        Duration.between(start, LocalDateTime.now()).toMillis());

            } catch (Exception ex) {
                log.error("❌ Price refresh job failed (duration: {} ms)",
                        Duration.between(start, LocalDateTime.now()).toMillis(),
                        ex);
            } finally {
                _scheduleLoadAndSavePrices.unlock();
            }
        } else {
            log.warn("⌛ Price refresh job rejected");
        }
    }

    public List<PriceEntity> loadAndSavePrices() {
        try {

            List<PriceEntity> prices = priceFetcher.loadAndSavePrices();
            if (prices.isEmpty()) return List.of();

            List<TickerEntity> updatedTickers =
                    prices.stream()
                            .map(PriceEntity::getTicker)
                            .toList();

            alertService.detectAndSaveAlerts(updatedTickers, SCAN_PRICES_MIN_COUNT);

            return prices;

        } catch (PriceFetchException ex) {
            log.error("Error prices fetching", ex);
            return List.of();
        }
    }
}
