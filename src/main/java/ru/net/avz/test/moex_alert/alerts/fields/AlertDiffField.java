package ru.net.avz.test.moex_alert.alerts.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Schema(description = "Движение цена, %", example = "0.57")
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface AlertDiffField {
    String message() default "Invalid diff value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
