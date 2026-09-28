package ru.net.avz.test.moex_alert.common;

import lombok.Builder;

@Builder
public record WebSocketEndpointSpec(
        String path,
        String operationTag,
        String operationId,
        String operationTitle,
        Class<?> messageType
) {
}
