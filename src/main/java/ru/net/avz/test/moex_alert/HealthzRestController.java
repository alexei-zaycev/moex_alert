package ru.net.avz.test.moex_alert;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Monitoring")
public class HealthzRestController {

    @GetMapping("/api/healthz")
    @Operation(operationId = "healthz")
    @ResponseStatus(HttpStatus.OK)
    public String healthz() {
        return "OK";
    }
}
