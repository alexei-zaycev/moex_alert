package ru.net.avz.test.moex_alert.common.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Positive;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Positive
@Schema(description = "Стоимость", example = "12.34")
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface SumAmountField {
    String message() default "Invalid amount";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
