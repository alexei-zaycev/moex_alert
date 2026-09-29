package ru.net.avz.test.moex_alert.common;

public interface ErrorCodes {

    String PARSING_ERROR = "parsing_error";
    String VALIDATION_ERROR = "validation_error";
    String DATA_INTEGRITY_VIOLATION = "data_integrity_violation";
    String INTERNAL_ERROR = "internal_error";

    String TICKER_NOT_FOUND = "ticker_not_found";
    String TICKER_ALREADY_EXISTS = "ticker_already_exists";
    String ALERT_NOT_FOUND = "alert_not_found";
}
