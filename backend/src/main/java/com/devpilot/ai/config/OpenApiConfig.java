package com.devpilot.ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI devPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevPilot AI — Developer Workspace API")
                        .description("REST API documentation for DevPilot AI, an intelligent cloud developer workspace with JWT authentication and RBAC.")
                        .version("1.0.0-rc")
                        .contact(new Contact()
                                .name("DevPilot Engineering Team")
                                .email("developer@devpilot.ai")
                                .url("https://github.com/shriprakash980/AI-Powered-Workspace"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token format: Bearer <token>")));
    }
}
