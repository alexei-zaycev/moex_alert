package ru.net.avz.test.moex_alert.alerts;

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
import ru.net.avz.test.moex_alert.alerts.dto.AlertDto;
import ru.net.avz.test.moex_alert.common.dto.ErrorResponseDto;
import ru.net.avz.test.moex_alert.common.fields.EntityIdField;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
@Validated
@Tag(name = "Alerts", description = "Операции с оповещениями")
public class AlertRestController {

    private final AlertService alertService;

    @GetMapping
    @Operation(operationId = "getAllAlerts", summary = "Получение списка оповещений")
    @ResponseStatus(HttpStatus.OK)
    public PagedModel<AlertDto> getAll(
            @Valid @ParameterObject Pageable pageable
    ) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(AlertEntity::getTs).ascending().and(Sort.by(AlertEntity::getId).ascending()));
        }
        Page<AlertEntity> alerts = alertService.findAll(pageable);
        return new PagedModel<>(alerts.map(AlertDto::of));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAlert", summary = "Получение оповещения по ID")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Оповещение не найдено", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public AlertDto getOne(
            @Valid @PathVariable @EntityIdField String id
    ) {
        AlertEntity alert = alertService.findByIdOrThrow(UUID.fromString(id));
        return AlertDto.of(alert);
    }
}