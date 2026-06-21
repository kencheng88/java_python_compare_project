package com.example.todo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Springdoc OpenAPI documentation generation.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * FastAPI automatically generates OpenAPI documentation out-of-the-box via Pydantic schemas,
 * mounting Swagger UI at {@code /docs} and ReDoc at {@code /redoc}. Customizing OpenAPI groups
 * or security requirements in FastAPI involves modifying properties of the {@code FastAPI} instance
 * or creating secondary sub-apps.
 * In Spring Boot, Springdoc OpenAPI is the standard tool for generating documentation. We configure
 * it via Java beans. We register a {@link SecurityScheme} (Bearer Token) to enable the "Authorize"
 * lock icon in Swagger. Additionally, we define a {@link GroupedOpenApi} for the "mcp" group, which
 * maps to the path {@code /v3/api-docs/mcp} and provides specific metadata tailored for Model Context
 * Protocol (MCP) clients.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define the global OpenAPI components and security schema.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Todo API Documentation")
                .version("1.0")
                .description("Spring Boot REST API for managing Todos, demonstrating security and transactional behavior."))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Opaque Token")
                        .description("Enter your personnel token (e.g., 'user-token' or 'admin-token')")));
    }

    /**
     * Group for public APIs. Exposes docs under /v3/api-docs/public
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/api/todos/**")
            .build();
    }

    /**
     * Group for Model Context Protocol (MCP) clients.
     * Exposes custom description docs under /v3/api-docs/mcp
     */
    @Bean
    public GroupedOpenApi mcpOpenApi() {
        return GroupedOpenApi.builder()
            .group("mcp")
            .pathsToMatch("/api/**")
            .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                .title("MCP Todo and Logging API")
                .version("1.0")
                .description("Machine-to-Machine context descriptions. Specially tailored for Model Context Protocol agents to view and inspect system state, write todos, and monitor transaction logs.")))
            .build();
    }
}
