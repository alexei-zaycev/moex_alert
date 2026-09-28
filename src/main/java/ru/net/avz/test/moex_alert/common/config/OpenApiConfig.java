package ru.net.avz.test.moex_alert.common.config;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

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

    @Bean
    public PropertyCustomizer metaSchemaPropertyCustomizer() {
        return (schema, annotatedType) -> {
            Annotation[] annotations = annotatedType.getCtxAnnotations();
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    // Ищем @Schema (из пакета io.swagger.v3.o3.models.annotations),
                    // повешенную на класс мета-аннотации
                    var meta = AnnotatedElementUtils.findMergedAnnotation(annotation.annotationType(), Schema.class);
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
