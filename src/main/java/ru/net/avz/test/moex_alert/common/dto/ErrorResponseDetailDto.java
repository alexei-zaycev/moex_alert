package ru.net.avz.test.moex_alert.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ErrorResponseDetailDto(

        @NotNull
        String field,

        @NotNull
        String code,

        @NotNull
        String message

) {
}
