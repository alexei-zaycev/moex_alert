package ru.net.avz.test.moex_alert.tickers.exceptions;

import org.springframework.http.HttpStatus;
import ru.net.avz.test.moex_alert.common.ErrorCodes;
import ru.net.avz.test.moex_alert.common.exceptions.EntityException;

public class TickerAlreadyExistsException
        extends EntityException {

    public static final HttpStatus HTTP_STATUS = HttpStatus.CONFLICT;

    public TickerAlreadyExistsException(String name) {
        super(TickerAlreadyExistsException.HTTP_STATUS, ErrorCodes.TICKER_ALREADY_EXISTS,
                "Ticker with name `%s` already exists".formatted(name));
    }
}
