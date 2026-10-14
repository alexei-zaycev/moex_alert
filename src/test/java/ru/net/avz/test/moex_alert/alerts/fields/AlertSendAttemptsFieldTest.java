package ru.net.avz.test.moex_alert.alerts.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AlertSendAttemptsFieldTest {

    @Test
    @DisplayName("Should have correct @Schema")
    void shouldHaveCorrectSchemaAnnotation() {
        Schema schema = AlertSendAttemptsField.class.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertNotNull(schema.description(), "description missed");
    }

    @Test
    @DisplayName("Should have correct @Target")
    void shouldHaveCorrectTargetAnnotation() {
        Target target = AlertSendAttemptsField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertThat(target.value()).contains(FIELD);
        assertThat(target.value()).contains(PARAMETER);
    }

    @Test
    @DisplayName("Should have @Constraint")
    void shouldHaveConstraintAnnotation() {
        assertTrue(AlertSendAttemptsField.class.isAnnotationPresent(Constraint.class));
    }

    @Test
    @DisplayName("Should have correct @Min")
    void shouldHaveCorrectMinAnnotation() {
        Min min = AlertSendAttemptsField.class.getAnnotation(Min.class);
        assertNotNull(min);
        assertEquals(0, min.value(), "value != 0");
    }

    @Test
    @DisplayName("Should have correct @Max")
    void shouldHaveCorrectMaxAnnotation() {
        Max max = AlertSendAttemptsField.class.getAnnotation(Max.class);
        assertNotNull(max);
        assertEquals(1000, max.value(), "value != 1000");
    }
}
