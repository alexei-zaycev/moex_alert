package ru.net.avz.test.moex_alert.tickers.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.tickers.TickerEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TickerPatchDtoTest {

    @Nested
    @DisplayName("Should create correct Builder")
    class shouldReturnCorrectBuilder {

        private static final String NAME = "XYZ";
        private static final Float THRESHOLD = 11.22f;

        @Test
        @DisplayName("if change name and threshold")
        void withNameAndThreshold() {

            TickerEntity ticker = TestHelper.createTickerBuilder().build();

            TickerPatchDto dto = new TickerPatchDto(
                    NAME,
                    THRESHOLD);

            TickerEntity.TickerEntityBuilder<?, ?> builder = dto.applyTo(ticker);
            assertNotNull(builder);

            TickerEntity updatedEntity = builder.build();
            assertEquals(dto.name(), updatedEntity.getName());
            assertEquals(dto.threshold(), updatedEntity.getThreshold());
            assertEquals(ticker.getCurrency(), updatedEntity.getCurrency());
        }

        @Test
        @DisplayName("if change name")
        void withName() {

            TickerEntity ticker = TestHelper.createTickerBuilder().build();

            TickerPatchDto dto = new TickerPatchDto(
                    NAME,
                    null);

            TickerEntity.TickerEntityBuilder<?, ?> builder = dto.applyTo(ticker);
            assertNotNull(builder);

            TickerEntity updatedEntity = builder.build();
            assertEquals(dto.name(), updatedEntity.getName());
            assertEquals(ticker.getThreshold(), updatedEntity.getThreshold());
            assertEquals(ticker.getCurrency(), updatedEntity.getCurrency());
        }

        @Test
        @DisplayName("if change threshold")
        void withThreshold() {

            TickerEntity ticker = TestHelper.createTickerBuilder().build();

            TickerPatchDto dto = new TickerPatchDto(
                    null,
                    THRESHOLD);

            TickerEntity.TickerEntityBuilder<?, ?> builder = dto.applyTo(ticker);
            assertNotNull(builder);

            TickerEntity updatedEntity = builder.build();
            assertEquals(ticker.getName(), updatedEntity.getName());
            assertEquals(dto.threshold(), updatedEntity.getThreshold());
            assertEquals(ticker.getCurrency(), updatedEntity.getCurrency());
        }
    }
}
