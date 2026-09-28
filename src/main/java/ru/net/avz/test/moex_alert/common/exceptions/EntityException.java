package ru.net.avz.test.moex_alert.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

public class EntityException extends RuntimeException {

    @Getter
    private final HttpStatusCode httpStatus;

    @Getter
    private final String errorCode;

    public EntityException(
            HttpStatusCode httpStatus,
            String errorCode,
            String message
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
