package ru.net.avz.test.moex_alert.prices.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMin;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@DecimalMin(value = "0", inclusive = false)
@Schema(description = "Стоимость", example = "12.34")
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface PriceAmountField {
    String message() default "Invalid amount";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
