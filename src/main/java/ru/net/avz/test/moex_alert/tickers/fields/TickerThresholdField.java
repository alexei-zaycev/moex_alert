package ru.net.avz.test.moex_alert.tickers.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Min(1)
@Max(100)
@Schema(description = "Порог срабатывания алерта, %", example = "20")
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface TickerThresholdField {
    String message() default "Invalid threshold";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
