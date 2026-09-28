package ru.net.avz.test.moex_alert.alerts.fields;

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

@Min(0)
@Max(1000)
@Schema(description = "Количество попыток отправки", example = "0")
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface AlertSendAttemptsField {
    String message() default "Invalid send attempts value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
