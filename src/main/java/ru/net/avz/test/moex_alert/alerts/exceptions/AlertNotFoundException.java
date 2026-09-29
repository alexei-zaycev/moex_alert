package ru.net.avz.test.moex_alert.alerts.exceptions;

import org.springframework.http.HttpStatus;
import ru.net.avz.test.moex_alert.common.ErrorCodes;
import ru.net.avz.test.moex_alert.common.exceptions.EntityException;

import java.util.UUID;

public class AlertNotFoundException extends EntityException {

    public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public AlertNotFoundException(UUID id) {
        super(AlertNotFoundException.HTTP_STATUS, ErrorCodes.ALERT_NOT_FOUND,
                "Alert with id `%s` not found".formatted(id));
    }
}
