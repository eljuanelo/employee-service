package com.invex.employee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    // Defines API metadata and the JWT Bearer scheme.
    @Bean
    public OpenAPI employeeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Service API")
                        .version("1.0.0")
                        .description("REST API for employee management with JWT authentication."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    // Documents both creation payload shapes and the birth date format.
    @Bean
    public OpenApiCustomiser employeeSchemas() {
        return openApi -> {
            var content = openApi.getPaths().get("/employees").getPost()
                    .getRequestBody().getContent().get("application/json");
            ArraySchema batch = (ArraySchema) content.getSchema();
            content.setSchema(new ComposedSchema()
                    .addOneOfItem(batch.getItems())
                    .addOneOfItem(batch.minItems(1)));

            for (String schemaName : new String[]{"EmployeeCreateRequest", "EmployeeUpdateRequest", "EmployeeResponse"}) {
                Schema<?> employee = openApi.getComponents().getSchemas().get(schemaName);
                employee.getProperties().get("birthDate").setFormat("dd-MM-yyyy");
            }
        };
    }
}
