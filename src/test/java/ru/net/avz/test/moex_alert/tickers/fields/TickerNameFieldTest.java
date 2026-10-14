package ru.net.avz.test.moex_alert.tickers.fields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TickerNameFieldTest {

    @Test
    @DisplayName("Should have correct @Schema")
    void shouldHaveCorrectSchemaAnnotation() {
        Schema schema = TickerNameField.class.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertNotNull(schema.description(), "description missed");
    }

    @Test
    @DisplayName("Should have correct @Target")
    void shouldHaveCorrectTargetAnnotation() {
        Target target = TickerNameField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertThat(target.value()).contains(FIELD);
        assertThat(target.value()).contains(PARAMETER);
    }

    @Test
    @DisplayName("Should have @Constraint")
    void shouldHaveConstraintAnnotation() {
        assertTrue(TickerNameField.class.isAnnotationPresent(Constraint.class));
    }

    @Test
    @DisplayName("Should have correct @Size")
    void shouldHaveCorrectSizeAnnotation() {
        Size size = TickerNameField.class.getAnnotation(Size.class);
        assertNotNull(size);
        assertEquals(1, size.min(), "min != 1");
        assertEquals(10, size.max(), "max != 10");
    }
}
