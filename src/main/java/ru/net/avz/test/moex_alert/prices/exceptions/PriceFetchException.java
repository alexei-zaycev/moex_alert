package ru.net.avz.test.moex_alert.prices.exceptions;

public class PriceFetchException extends RuntimeException {

    public PriceFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public PriceFetchException(Throwable cause) {
        super(cause);
    }
}
