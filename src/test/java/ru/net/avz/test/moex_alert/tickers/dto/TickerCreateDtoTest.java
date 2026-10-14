package ru.net.avz.test.moex_alert.tickers.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import static org.junit.jupiter.api.Assertions.*;

class TickerCreateDtoTest {

    private static final String NAME = "XYZ";
    private static final Float THRESHOLD = 11.22f;

    @Test
    @DisplayName("Should create correct Builder")
    void shouldReturnCorrectBuilder() {

        TickerCreateDto dto = new TickerCreateDto(
                NAME,
                THRESHOLD);

        TickerEntity.TickerEntityBuilder<?, ?> builder = dto.toBuilder();
        assertNotNull(builder);

        TickerEntity createdEntity = builder.build();
        assertNull(createdEntity.getId());
        assertNull(createdEntity.getCreatedAt());
        assertNull(createdEntity.getUpdatedAt());
        assertEquals(dto.name(), createdEntity.getName());
        assertEquals(dto.threshold(), createdEntity.getThreshold());
        assertEquals("RUB", createdEntity.getCurrency());
    }
}
