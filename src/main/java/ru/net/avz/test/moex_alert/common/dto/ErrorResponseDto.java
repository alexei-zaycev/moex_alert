package ru.net.avz.test.moex_alert.common.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;
import ru.net.avz.test.moex_alert.common.GlobalRestExceptionHandler;

record ErrorResponseDetailDto(

        @NotNull
        String field,

        @NotNull
        String code,

        @NotNull
        String message

) {
}

/**
 * @see ErrorResponseException
 * @see GlobalRestExceptionHandler#_newResponse(HttpStatusCode, String, Object[], Exception)
 */
public record ErrorResponseDto(

        @NotNull
        String instance,

        @NotNull
        int status,

        @NotNull
        String title,

        @Nullable
        String detail,

        @Nullable
        ErrorResponseDetailDto[] errors

) {
}
