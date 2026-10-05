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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/tickers")
@Validated
@Tag(name = "Tickers", description = "Операции с тикерами")
public class TickerRestController {

    private final TickerService tickerService;

    @GetMapping
    @Operation(operationId = "getAllTickers", summary = "Получение списка тикеров")
    @ResponseStatus(HttpStatus.OK)
    public PagedModel<TickerDto> getAll(
            @Valid @ParameterObject Pageable pageable) {

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(TickerEntity::getName).ascending().and(Sort.by(TickerEntity::getId).ascending()));
        }
        Page<TickerEntity> tickers = tickerService.findAll(pageable);
        return new PagedModel<>(tickers.map(TickerDto::of));
    }

    @GetMapping("/{name}")
    @Operation(operationId = "getTicker", summary = "Получение тикера по имени")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto getOne(
            @Valid @PathVariable @TickerNameField String name) {

        TickerEntity ticker = tickerService.findByNameOrThrow(name);
        return TickerDto.of(ticker);
    }

    @PostMapping
    @Operation(operationId = "createTicker", summary = "Создание нового тикера")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
            @ApiResponse(responseCode = "409", description = "Тикер уже существует", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка формата", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto create(
            @Valid @RequestBody TickerCreateDto dto) {

        TickerEntity ticker = tickerService.create(dto);
        return TickerDto.of(ticker);
    }

    @PutMapping("/{name}")
    @Operation(operationId = "updateTicker", summary = "Обновление тикера")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка формата", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto update(
            @Valid @PathVariable @TickerNameField String name,
            @Valid @RequestBody TickerUpdateDto dto) {

        TickerEntity ticker = tickerService.update(name, dto);
        return TickerDto.of(ticker);
    }

    @PatchMapping("/{name}")
    @Operation(operationId = "patchTicker", summary = "Частичное обновление тикера")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка формата", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public TickerDto patch(
            @Valid @PathVariable @TickerNameField String name,
            @Valid @RequestBody TickerPatchDto dto) {

        TickerEntity ticker = tickerService.patch(name, dto);
        return TickerDto.of(ticker);
    }

    @DeleteMapping("/{name}")
    @Operation(operationId = "deleteTicker", summary = "Удаление тикера")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Тикер не найден", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public void delete(
            @Valid @PathVariable @TickerNameField String name) {

        tickerService.delete(name);
    }
}