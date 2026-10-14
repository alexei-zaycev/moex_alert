package ru.net.avz.test.moex_alert.alerts.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.alerts.AlertEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AlertWebSocketDtoTest {

    @Test
    @DisplayName("Should create correct DTO from valid Entity")
    void shouldCreateDtoFromValidEntity() {

        AlertEntity entity = TestHelper.createAlertBuilder().build();
        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        AlertWebSocketDto dto = new AlertWebSocketDto(entity);
        assertEquals(entity.getTicker().getName(), dto.ticker);
        assertEquals(entity.getTs(), dto.ts);
        assertEquals(entity.getAmount(), dto.amount);
        assertEquals(entity.getCurrency(), dto.currency);
        assertEquals(entity.getDiff(), dto.diff);
    }
}
