package ru.net.avz.test.moex_alert.prices.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.prices.PriceEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriceDtoTest {

    @Test
    @DisplayName("Should create correct DTO from valid Entity")
    void shouldCreateDtoFromValidEntity() {

        PriceEntity entity = TestHelper.createPriceBuilder().build();
        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        PriceDto dto = new PriceDto(entity);
        assertEquals(entity.getTs(), dto.ts);
        assertEquals(entity.getAmount(), dto.amount);
        assertEquals(entity.getCurrency(), dto.currency);
    }
}
