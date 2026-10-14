package ru.net.avz.test.moex_alert.tickers.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TickerThresholdFieldTest {

    @Test
    @DisplayName("Should have correct @Schema")
    void shouldHaveCorrectSchemaAnnotation() {
        Schema schema = TickerThresholdField.class.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertNotNull(schema.description(), "description missed");
    }

    @Test
    @DisplayName("Should have correct @Target")
    void shouldHaveCorrectTargetAnnotation() {
        Target target = TickerThresholdField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertThat(target.value()).contains(FIELD);
        assertThat(target.value()).contains(PARAMETER);
    }

    @Test
    @DisplayName("Should have @Constraint")
    void shouldHaveConstraintAnnotation() {
        assertTrue(TickerThresholdField.class.isAnnotationPresent(Constraint.class));
    }

    @Test
    @DisplayName("Should have @Positive")
    void shouldHavePositiveAnnotation() {
        assertTrue(TickerThresholdField.class.isAnnotationPresent(Positive.class));
    }

    @Test
    @DisplayName("Should have correct @Max")
    void shouldHaveCorrectMaxAnnotation() {
        Max max = TickerThresholdField.class.getAnnotation(Max.class);
        assertNotNull(max);
        assertEquals(100, max.value(), "value != 100");
    }
}
