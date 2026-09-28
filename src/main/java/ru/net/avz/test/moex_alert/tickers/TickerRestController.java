package ru.net.avz.test.moex_alert.tickers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.net.avz.test.moex_alert.common.dto.ErrorResponseDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerCreateDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerPatchDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerUpdateDto;
import ru.net.avz.test.moex_alert.tickers.fields.TickerNameField;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tickers")
@Validated
@Tag(name = "tickers", description = "Операции с тикерами")
public class TickerRestController {

    private final TickerService tickerService;

    @GetMapping
    @Operation(summary = "Получить список тикеров")
    @ResponseStatus(HttpStatus.OK)
    public PagedModel<TickerDto> getAll(
            @Valid @ParameterObject Pageable pageable
    ) {
        Page<TickerEntity> tickers = tickerService.findAll(pageable);
        return new PagedModel<>(tickers.map(TickerDto::of));
    }

    @GetMapping("/{name}")
    @Operation(summary = "Получить актив по тикеру")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto getOne(
            @Valid @PathVariable @TickerNameField String name
    ) {
        TickerEntity entity = tickerService.findOneByNameOrThrow(name);
        return TickerDto.of(entity);
    }

    @PostMapping
    @Operation(summary = "Создать новый тикер")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
            @ApiResponse(responseCode = "409", description = "Тикер уже существует", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка формата", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto create(
            @Valid @RequestBody TickerCreateDto dto
    ) {
        TickerEntity entity = tickerService.create(dto);
        return TickerDto.of(entity);
    }

    @PutMapping("/{name}")
    @Operation(summary = "Обновить тикер")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка формата", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto update(
            @Valid @PathVariable @TickerNameField String name,
            @Valid @RequestBody TickerUpdateDto dto
    ) {
        TickerEntity entity = tickerService.update(name, dto);
        return TickerDto.of(entity);
    }

    @PatchMapping("/{name}")
    @Operation(summary = "Частично обновить тикер")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка формата", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto patch(
            @Valid @PathVariable @TickerNameField String name,
            @Valid @RequestBody TickerPatchDto dto
    ) {
        TickerEntity entity = tickerService.patch(name, dto);
        return TickerDto.of(entity);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Удалить тикер")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public void delete(
            @Valid @PathVariable @TickerNameField String name
    ) {
        tickerService.delete(name);
    }
}