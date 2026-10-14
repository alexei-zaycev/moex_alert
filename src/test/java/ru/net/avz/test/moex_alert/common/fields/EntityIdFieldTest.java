package ru.net.avz.test.moex_alert.common.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import org.hibernate.validator.constraints.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityIdFieldTest {

    @Test
    @DisplayName("Should have correct @Schema")
    void shouldHaveCorrectSchemaAnnotation() {
        Schema schema = EntityIdField.class.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertNotNull(schema.description(), "description missed");
    }

    @Test
    @DisplayName("Should have correct @Target")
    void shouldHaveCorrectTargetAnnotation() {
        Target target = EntityIdField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertThat(target.value()).contains(FIELD);
        assertThat(target.value()).contains(PARAMETER);
    }

    @Test
    @DisplayName("Should have @Constraint")
    void shouldHaveConstraintAnnotation() {
        assertTrue(EntityIdField.class.isAnnotationPresent(Constraint.class));
    }

    @Test
    @DisplayName("Should have @UUID")
    void shouldHaveUuidAnnotation() {
        assertTrue(EntityIdField.class.isAnnotationPresent(UUID.class));
    }
}
