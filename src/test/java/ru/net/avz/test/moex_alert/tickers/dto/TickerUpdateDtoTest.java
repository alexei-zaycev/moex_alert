package ru.net.avz.test.moex_alert.tickers.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TickerUpdateDtoTest {

    private static final String NAME = "XYZ";
    private static final Float THRESHOLD = 11.22f;

    @Test
    @DisplayName("Should create correct Builder")
    void shouldReturnCorrectBuilder() {

        TickerEntity ticker = TestHelper.createTickerBuilder().build();

        TickerUpdateDto dto = new TickerUpdateDto(
                NAME,
                THRESHOLD);

        TickerEntity.TickerEntityBuilder builder = dto.applyTo(ticker);
        assertNotNull(builder);

        TickerEntity updatedEntity = builder.build();
        assertEquals(dto.name(), updatedEntity.getName());
        assertEquals(dto.threshold(), updatedEntity.getThreshold());
        assertEquals(ticker.getCurrency(), updatedEntity.getCurrency());
    }
}
