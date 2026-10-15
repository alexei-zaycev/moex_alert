package ru.net.avz.test.moex_alert.tickers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.net.avz.test.moex_alert.TestHelper;
import ru.net.avz.test.moex_alert.tickers.dto.TickerCreateDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerPatchDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerUpdateDto;
import ru.net.avz.test.moex_alert.tickers.exceptions.TickerAlreadyExistsException;
import ru.net.avz.test.moex_alert.tickers.exceptions.TickerNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TickerServiceTest {

    private TickerService tickerService;

    private TickerRepository tickerRepository;
    @Captor private ArgumentCaptor<TickerEntity> tickerCaptor;

    @BeforeEach
    void setUp() {
        tickerRepository = mock(TickerRepository.class);
        tickerService = new TickerService(tickerRepository);
    }

    private void assertTickersIsSame(TickerEntity expected, TickerEntity actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getThreshold(), actual.getThreshold());
        assertEquals(expected.getCurrency(), actual.getCurrency());
    }

    @Nested
    @DisplayName("findByNameOrThrow()")
    class FindByNameOrThrowTests {

        @Test
        @DisplayName("Should return ticker when found by name")
        void findByNameOrThrow_tickerExists_returnsTicker() {

            TickerEntity ticker = TestHelper.asSaved(TestHelper.createTickerBuilder("SBER")).build();

            when(tickerRepository.findByName(ticker.getName())).thenReturn(Optional.of(ticker));

            TickerEntity result = tickerService.findByNameOrThrow(ticker.getName());
            assertTickersIsSame(ticker, result);

            verify(tickerRepository, times(1)).findByName(ticker.getName());
        }

        @Test
        @DisplayName("Should throw TickerNotFoundException when ticker does not exist")
        void findByNameOrThrow_tickerNotFound_throwsException() {

            String nonExistentName = "LKOH";

            when(tickerRepository.findByName(nonExistentName)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tickerService.findByNameOrThrow(nonExistentName))
                    .isInstanceOf(TickerNotFoundException.class);

            verify(tickerRepository, times(1)).findByName(nonExistentName);
        }
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Should throw TickerAlreadyExistsException when ticker with same name already exists")
        void create_existingTicker_throwsException() {

            String tickerName = "SBER";
            TickerEntity existingTicker = TestHelper.asSaved(TestHelper.createTickerBuilder(tickerName)).build();
            TickerCreateDto createDto = new TickerCreateDto(tickerName, 0.12f);

            when(tickerRepository.findByName(tickerName)).thenReturn(Optional.of(existingTicker));

            assertThatThrownBy(() -> tickerService.create(createDto))
                    .isInstanceOf(TickerAlreadyExistsException.class);

            verify(tickerRepository, times(1)).findByName(tickerName);
            verify(tickerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create a new ticker when it does not exist")
        void create_newTicker_savesAndReturns() {

            TickerCreateDto createDto = new TickerCreateDto("SBER", 0.12f);
            TickerEntity expectedTicker = TestHelper.createTickerBuilder()
                    .name(createDto.name())
                    .threshold(createDto.threshold())
                    .currency(TestHelper.DEFAULT_CURRENCY)
                    .build();

            when(tickerRepository.findByName(createDto.name())).thenReturn(Optional.empty());
            when(tickerRepository.save(any(TickerEntity.class))).thenAnswer(TestHelper.repositorySaveStub);

            TickerEntity result = tickerService.create(createDto);
            assertTickersIsSame(expectedTicker, result);

            verify(tickerRepository, times(1)).save(tickerCaptor.capture());
            assertTickersIsSame(expectedTicker, tickerCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Should throw TickerNotFoundException when updating non-existent ticker")
        void update_nonExistentTicker_throwsException() {

            String nonExistentName = "LKOH";
            TickerUpdateDto updateDto = new TickerUpdateDto("SBER", 0.5f);

            when(tickerRepository.findByName(nonExistentName)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tickerService.update(nonExistentName, updateDto))
                    .isInstanceOf(TickerNotFoundException.class);

            verify(tickerRepository, times(1)).findByName(nonExistentName);
            verify(tickerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fully update an existing ticker")
        void update_existingTicker_updatesAndReturns() {

            TickerEntity existingTicker = TestHelper.asSaved(
                    TestHelper.createTickerBuilder()
                            .name("SBER")
                            .threshold(0.12f)
                            .currency(TestHelper.DEFAULT_CURRENCY)
            ).build();

            TickerUpdateDto updateDto = new TickerUpdateDto("LKOH", 0.99f);
            TickerEntity expectedTicker = TestHelper.createTickerBuilder()
                    .id(existingTicker.getId())
                    .name(updateDto.name())
                    .threshold(updateDto.threshold())
                    .currency(existingTicker.getCurrency())
                    .build();

            when(tickerRepository.findByName(existingTicker.getName())).thenReturn(Optional.of(existingTicker));
            when(tickerRepository.save(any(TickerEntity.class))).thenAnswer(TestHelper.repositorySaveStub);

            TickerEntity result = tickerService.update(existingTicker.getName(), updateDto);
            assertTickersIsSame(expectedTicker, result);

            verify(tickerRepository, times(1)).save(tickerCaptor.capture());
            assertTickersIsSame(expectedTicker, tickerCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("patch()")
    class PatchTests {

        @Test
        @DisplayName("Should throw TickerNotFoundException when patching non-existent ticker")
        void patch_nonExistentTicker_throwsException() {

            String nonExistentName = "LKOH";
            TickerPatchDto patchDto = new TickerPatchDto("SBER", 0.5f);

            when(tickerRepository.findByName(nonExistentName)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tickerService.patch(nonExistentName, patchDto))
                    .isInstanceOf(TickerNotFoundException.class);

            verify(tickerRepository, times(1)).findByName(nonExistentName);
            verify(tickerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should partially update ticker when only name is provided")
        void patch_withOnlyName_updatesNameOnly() {

            TickerEntity existingTicker = TestHelper.asSaved(
                    TestHelper.createTickerBuilder()
                            .name("SBER")
                            .threshold(0.12f)
                            .currency(TestHelper.DEFAULT_CURRENCY)
            ).build();

            TickerPatchDto patchDto = new TickerPatchDto("LKOH", null);
            TickerEntity expectedTicker = TestHelper.createTickerBuilder()
                    .id(existingTicker.getId())
                    .name(patchDto.name())
                    .threshold(existingTicker.getThreshold())
                    .currency(existingTicker.getCurrency())
                    .build();

            when(tickerRepository.findByName(existingTicker.getName())).thenReturn(Optional.of(existingTicker));
            when(tickerRepository.save(any(TickerEntity.class))).thenAnswer(TestHelper.repositorySaveStub);

            TickerEntity result = tickerService.patch(existingTicker.getName(), patchDto);
            assertTickersIsSame(expectedTicker, result);

            verify(tickerRepository, times(1)).save(tickerCaptor.capture());
            assertTickersIsSame(expectedTicker, tickerCaptor.getValue());
        }

        @Test
        @DisplayName("Should partially update ticker when only threshold is provided")
        void patch_withOnlyThreshold_updatesThresholdOnly() {

            TickerEntity existingTicker = TestHelper.asSaved(
                    TestHelper.createTickerBuilder()
                            .name("SBER")
                            .threshold(0.12f)
                            .currency(TestHelper.DEFAULT_CURRENCY)
            ).build();

            TickerPatchDto patchDto = new TickerPatchDto(null, 0.5f);
            TickerEntity expectedTicker = TestHelper.createTickerBuilder()
                    .id(existingTicker.getId())
                    .name(existingTicker.getName())
                    .threshold(patchDto.threshold())
                    .currency(existingTicker.getCurrency())
                    .build();

            when(tickerRepository.findByName(existingTicker.getName())).thenReturn(Optional.of(existingTicker));
            when(tickerRepository.save(any(TickerEntity.class))).thenAnswer(TestHelper.repositorySaveStub);

            TickerEntity result = tickerService.patch(existingTicker.getName(), patchDto);
            assertTickersIsSame(expectedTicker, result);

            verify(tickerRepository, times(1)).save(tickerCaptor.capture());
            assertTickersIsSame(expectedTicker, tickerCaptor.getValue());
        }

        @Test
        @DisplayName("Should update both name and threshold when both are provided")
        void patch_withBothFields_updatesBoth() {

            TickerEntity existingTicker = TestHelper.asSaved(
                    TestHelper.createTickerBuilder()
                            .name("SBER")
                            .threshold(0.12f)
                            .currency(TestHelper.DEFAULT_CURRENCY)
            ).build();

            TickerPatchDto patchDto = new TickerPatchDto("LKOH", 0.5f);
            TickerEntity expectedTicker = TestHelper.createTickerBuilder()
                    .id(existingTicker.getId())
                    .name(patchDto.name())
                    .threshold(patchDto.threshold())
                    .currency(existingTicker.getCurrency())
                    .build();

            when(tickerRepository.findByName(existingTicker.getName())).thenReturn(Optional.of(existingTicker));
            when(tickerRepository.save(any(TickerEntity.class))).thenAnswer(TestHelper.repositorySaveStub);

            TickerEntity result = tickerService.patch(existingTicker.getName(), patchDto);
            assertTickersIsSame(expectedTicker, result);

            verify(tickerRepository, times(1)).save(tickerCaptor.capture());
            assertTickersIsSame(expectedTicker, tickerCaptor.getValue());
        }

        @Test
        @DisplayName("Should not update field when patch value is null")
        void patch_withNullValues_preservesOriginalFields() {

            TickerEntity existingTicker = TestHelper.asSaved(
                    TestHelper.createTickerBuilder()
                            .name("SBER")
                            .threshold(0.12f)
                            .currency(TestHelper.DEFAULT_CURRENCY)
            ).build();

            TickerPatchDto patchDto = new TickerPatchDto(null, null);

            when(tickerRepository.findByName(existingTicker.getName())).thenReturn(Optional.of(existingTicker));
            when(tickerRepository.save(any(TickerEntity.class))).thenAnswer(TestHelper.repositorySaveStub);

            TickerEntity result = tickerService.patch(existingTicker.getName(), patchDto);
            assertTickersIsSame(existingTicker, result);

            verify(tickerRepository, times(1)).save(tickerCaptor.capture());
            assertTickersIsSame(existingTicker, tickerCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should throw TickerNotFoundException when deleting non-existent ticker")
        void delete_nonExistentTicker_throwsException() {

            String nonExistentName = "LKOH";

            when(tickerRepository.findByName(nonExistentName)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tickerService.delete(nonExistentName))
                    .isInstanceOf(TickerNotFoundException.class);

            verify(tickerRepository, times(1)).findByName(nonExistentName);
            verify(tickerRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should delete ticker by name")
        void delete_existingTicker_deletesById() {

            String tickerName = "SBER";
            TickerEntity existingTicker = TestHelper.asSaved(TestHelper.createTickerBuilder(tickerName)).build();

            when(tickerRepository.findByName(tickerName)).thenReturn(Optional.of(existingTicker));

            tickerService.delete(tickerName);

            verify(tickerRepository, times(1)).findByName(tickerName);
            verify(tickerRepository, times(1)).deleteById(existingTicker.getId());
        }
    }
}
