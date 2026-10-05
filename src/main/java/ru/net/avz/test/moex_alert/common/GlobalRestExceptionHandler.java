package ru.net.avz.test.moex_alert.common;

import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.net.avz.test.moex_alert.common.dto.ErrorResponseDetailDto;
import ru.net.avz.test.moex_alert.common.dto.ErrorResponseDto;
import ru.net.avz.test.moex_alert.common.exceptions.EntityException;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;
import ru.net.avz.test.moex_alert.tickers.exceptions.TickerAlreadyExistsException;

import java.lang.annotation.Annotation;
import java.util.Optional;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    // ошибки парсинга
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleNotReadable(HttpMessageNotReadableException ex) {
        return ErrorResponseDto.newException(HttpStatus.BAD_REQUEST, ErrorCodes.PARSING_ERROR, null, ex);
    }

    // ошибки валидации
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleValidation(MethodArgumentNotValidException ex) {
        return ErrorResponseDto.newException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.VALIDATION_ERROR,
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err -> ErrorResponseDetailDto.builder()
                                .field(err.getField())
                                .code(Optional.ofNullable(err.getCode())
                                        .map(String::toUpperCase)
                                        .orElse("?"))
                                .message(err.getDefaultMessage())
                                .build())
                        .toList(),
                null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleValidation(ConstraintViolationException ex) {
        return ErrorResponseDto.newException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.VALIDATION_ERROR,
                ex.getConstraintViolations()
                        .stream()
                        .map(err ->
                                ErrorResponseDetailDto.builder()
                                        .field(err.getPropertyPath().toString())
                                        .code(Optional.ofNullable(err.getConstraintDescriptor())
                                                .map(ConstraintDescriptor::getAnnotation)
                                                .map(Annotation::annotationType)
                                                .map(Class::getSimpleName)
                                                .orElse("?"))
                                        .message(err.getMessage())
                                        .build())
                        .toList(),
                null);
    }

    // ошибки целостности БД
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponseException handleDataIntegrity(DataIntegrityViolationException ex) {
        @Nullable String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.toLowerCase().contains(TickerEntity.IDX_UNIQUE_NAME.toLowerCase())) {
            return ErrorResponseDto.newException(TickerAlreadyExistsException.HTTP_STATUS, ErrorCodes.TICKER_ALREADY_EXISTS, null, ex);
        }
        return ErrorResponseDto.newException(HttpStatus.CONFLICT, ErrorCodes.DATA_INTEGRITY_VIOLATION, null, ex);
    }


    // любое исключение сущности
    @ExceptionHandler(EntityException.class)
    public ErrorResponseException handleAssetNotFound(EntityException ex) {
        return ErrorResponseDto.newException(ex.getHttpStatus(), ex.getErrorCode(), null, ex);
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
        return ErrorResponseDto.newException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, null, ex);
    }
}

