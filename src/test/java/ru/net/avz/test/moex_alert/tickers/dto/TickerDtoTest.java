package ru.net.avz.test.moex_alert.tickers.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TickerDtoTest {

    @Test
    @DisplayName("Should create correct DTO from valid Entity")
    void shouldCreateDtoFromValidEntity() {

        TickerEntity entity = TestHelper.createTickerBuilder().build();
        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        TickerDto dto = new TickerDto(entity);
        assertEquals(entity.getName(), dto.name);
        assertEquals(entity.getThreshold(), dto.threshold);
        assertEquals(entity.getCurrency(), dto.currency);
    }
}
