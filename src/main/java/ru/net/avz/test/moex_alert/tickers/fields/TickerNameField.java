package ru.net.avz.test.moex_alert.tickers.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@NotBlank
@Size(max = 10)
@Schema(description = "Тикер актива", example = "SBER")
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface TickerNameField {
    String message() default "Invalid ticker";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
