package ru.net.avz.test.moex_alert.common.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.util.List;

@Builder
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
        public static ErrorResponseException newException(
                HttpStatusCode httpStatus,
                String errorCode,
                @Nullable List<ErrorResponseDetailDto> errorDetails,
                @Nullable Exception ex
        ) {

                ProblemDetail body = ProblemDetail.forStatus(httpStatus);
                body.setTitle(errorCode);
                if (errorDetails != null) body.setProperty("errors", errorDetails);
                if (ex != null) body.setDetail(ex.getMessage());

                return new ErrorResponseException(httpStatus, body, ex);
        }
}
