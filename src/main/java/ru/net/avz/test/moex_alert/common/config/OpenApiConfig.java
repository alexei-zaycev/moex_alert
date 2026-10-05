package ru.net.avz.test.moex_alert.common.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import ru.net.avz.test.moex_alert.alerts.sender.AlertWebSocketHandler;
import ru.net.avz.test.moex_alert.common.WebSocketEndpointSpec;

import java.lang.annotation.Annotation;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MOEX Alert API")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Zaitsev Alexei")
                                .email("alexei.zaycev@yandex.ru"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
//                .schemaRequirement("bearer-key", new io.swagger.v3.oas.models.security.SecurityScheme());
    }

    private OpenApiCustomizer _openApiWebSocketCustomizer(
            WebSocketEndpointSpec endpoint) {

        return openApi -> {

            boolean openapi31 = openApi.getSpecVersion() == SpecVersion.V31;
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            ModelConverters.getInstance(openapi31)
                    .readAll(endpoint.messageType())
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

            // Ищем @io.swagger.v3.oas.annotations.media.Schema, повешенную на DTO
            var meta = AnnotatedElementUtils.findMergedAnnotation(
                    endpoint.messageType(),
                    io.swagger.v3.oas.annotations.media.Schema.class);
            String schemaName = (meta != null && !meta.name().isBlank())
                    ? meta.name()
                    : endpoint.messageType().getSimpleName();

            openApi.path(endpoint.path(), new PathItem()
                    .get(new Operation()
                            .addTagsItem(endpoint.operationTag())
                            .operationId(endpoint.operationId())
                            .summary("WebSocket: %s".formatted(endpoint.operationTitle()))
                            .description("Протокол WebSocket. URL: `ws://{host}:{port}%s`.".formatted(endpoint.path()))
                            .responses(new ApiResponses()
                                    .addApiResponse("101", new ApiResponse()
                                            .description("Switching Protocols — WebSocket установка соединения"))
                                    .addApiResponse("200", new ApiResponse()
                                            .description("Server to client push")
                                            .content(new Content()
                                                    .addMediaType("application/json",
                                                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/%s".formatted(schemaName)))))))));
        };
    }

    @Bean
    public OpenApiCustomizer wsAlertsOpenApi() {
        return _openApiWebSocketCustomizer(AlertWebSocketHandler.ENDPOINT);
    }

    @Bean
    public PropertyCustomizer metaSchemaPropertyCustomizer() {
        return (schema, annotatedType) -> {
            Annotation[] annotations = annotatedType.getCtxAnnotations();
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    // Ищем @io.swagger.v3.oas.annotations.media.Schema, повешенную на класс мета-аннотации
                    var meta = AnnotatedElementUtils.findMergedAnnotation(
                            annotation.annotationType(),
                            io.swagger.v3.oas.annotations.media.Schema.class);
                    if (meta != null) {
                        if (!meta.description().isEmpty()) {
                            schema.setDescription(meta.description());
                        }
                        if (!meta.example().isEmpty()) {
                            schema.setExample(meta.example());
                        }
                        if (meta.nullable()) {
                            schema.setNullable(true);
                        }
                    }
                }
            }
            return schema;
        };
    }
}
