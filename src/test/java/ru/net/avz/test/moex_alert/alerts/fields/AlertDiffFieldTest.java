package ru.net.avz.test.moex_alert.alerts.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertDiffFieldTest {

    @Test
    @DisplayName("Should have correct @Schema")
    void shouldHaveCorrectSchemaAnnotation() {
        Schema schema = AlertDiffField.class.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertNotNull(schema.description(), "description missed");
    }

    @Test
    @DisplayName("Should have correct @Target")
    void shouldHaveCorrectTargetAnnotation() {
        Target target = AlertDiffField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertThat(target.value()).contains(FIELD);
        assertThat(target.value()).contains(PARAMETER);
    }

    @Test
    @DisplayName("Should have @Constraint")
    void shouldHaveConstraintAnnotation() {
        assertTrue(AlertDiffField.class.isAnnotationPresent(Constraint.class));
    }
}
