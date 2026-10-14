package ru.net.avz.test.moex_alert.common.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SumCurrencyFieldTest {

    @Test
    @DisplayName("Should have correct @Schema")
    void shouldHaveCorrectSchemaAnnotation() {
        Schema schema = SumCurrencyField.class.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertNotNull(schema.description(), "description missed");
    }

    @Test
    @DisplayName("Should have correct @Target")
    void shouldHaveCorrectTargetAnnotation() {
        Target target = SumCurrencyField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertThat(target.value()).contains(FIELD);
        assertThat(target.value()).contains(PARAMETER);
    }

    @Test
    @DisplayName("Should have @Constraint")
    void shouldHaveConstraintAnnotation() {
        assertTrue(SumCurrencyField.class.isAnnotationPresent(Constraint.class));
    }

    @Test
    @DisplayName("Should have @NotBlank")
    void shouldHaveNotBlankAnnotation() {
        assertTrue(SumCurrencyField.class.isAnnotationPresent(NotBlank.class));
    }

    @Test
    @DisplayName("Should have correct @Size")
    void shouldHaveCorrectSizeAnnotation() {
        Size size = SumCurrencyField.class.getAnnotation(Size.class);
        assertNotNull(size);
        assertEquals(3, size.min(), "min != 3");
        assertEquals(3, size.max(), "max != 3");
    }
}
