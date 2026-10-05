package ru.net.avz.test.moex_alert.tickers.exceptions;

import org.springframework.http.HttpStatus;
import ru.net.avz.test.moex_alert.common.ErrorCodes;
import ru.net.avz.test.moex_alert.common.exceptions.EntityException;

public class TickerNotFoundException
        extends EntityException {

    public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public TickerNotFoundException(String name) {
        super(TickerNotFoundException.HTTP_STATUS, ErrorCodes.TICKER_NOT_FOUND,
                "Ticker with name `%s` not found".formatted(name));
    }
}
