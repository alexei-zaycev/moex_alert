package ru.net.avz.test.moex_alert.alerts.sender;

import ru.net.avz.test.moex_alert.alerts.AlertEntity;

import java.util.concurrent.CompletableFuture;

/**
 * интеграция с доставкой оповещений
 */
public interface AlertSender {

    CompletableFuture<Boolean> sendAlert(
            AlertEntity alert);

}
