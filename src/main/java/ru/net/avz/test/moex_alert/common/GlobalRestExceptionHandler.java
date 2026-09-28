package ru.net.avz.test.moex_alert.common;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.net.avz.test.moex_alert.common.dto.ErrorResponseDto;
import ru.net.avz.test.moex_alert.common.exceptions.EntityException;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.exceptions.TickerAlreadyExistsException;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalRestExceptionHandler {


    /** @see ErrorResponseDto */
    private ErrorResponseException _newResponse(
            HttpStatusCode httpStatus,
            String errorCode,
            @Nullable Object[] errorDetails,
            @Nullable Exception ex
    ) {

        ProblemDetail body = ProblemDetail.forStatus(httpStatus);
        body.setTitle(errorCode);
        if (errorDetails != null) body.setProperty("errors", errorDetails);
        if (ex != null) body.setDetail(ex.getMessage());

        return new ErrorResponseException(httpStatus, body, ex);
    }

    // ошибки парсинга
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleNotReadable(HttpMessageNotReadableException ex) {
        return _newResponse(HttpStatus.BAD_REQUEST, ErrorCodes.PARSING_ERROR, null, ex);
    }

    // ошибки валидации
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleValidation(MethodArgumentNotValidException ex) {
        return _newResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.VALIDATION_ERROR,
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err -> Stream.of(
                                        new AbstractMap.SimpleImmutableEntry<String, Object>("field", err.getField()),
                                        new AbstractMap.SimpleImmutableEntry<String, Object>("code", err.getCode() != null ? err.getCode().toUpperCase() : null),
                                        new AbstractMap.SimpleImmutableEntry<String, Object>("message", err.getDefaultMessage()))
                                .filter(e -> e.getValue() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                        .toArray(),
                null);
    }

    // ошибки целостности БД
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponseException handleDataIntegrity(DataIntegrityViolationException ex) {
        @Nullable String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains(TickerEntity.IDX_UNIQUE_NAME)) {
            return _newResponse(TickerAlreadyExistsException.HTTP_STATUS, ErrorCodes.TICKER_ALREADY_EXISTS, null, ex);
        }
        return _newResponse(HttpStatus.CONFLICT, ErrorCodes.DATA_INTEGRITY_VIOLATION, null, ex);
    }


    // любое исключение сущности
    @ExceptionHandler(EntityException.class)
    public ErrorResponseException handleAssetNotFound(EntityException ex) {
        return _newResponse(ex.getHttpStatus(), ex.getErrorCode(), null, ex);
    }

    // любое исключение HTTP
    @ExceptionHandler(ErrorResponseException.class)
    public ErrorResponseException handleHttpException(ErrorResponseException ex) {
        return ex;
    }

    // любое другое исключение
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseException handleGenericException(Exception ex) {
        return _newResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, null, ex);
    }
}

