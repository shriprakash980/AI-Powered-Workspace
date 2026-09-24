package com.devpilot.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI devPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevPilot AI — Developer Workspace API")
                        .description("REST API documentation for DevPilot AI, an intelligent cloud developer workspace developed as a final-year B.Tech CSE project.")
                        .version("1.0.0-rc")
                        .contact(new Contact()
                                .name("DevPilot Engineering Team")
                                .email("developer@devpilot.ai")
                                .url("https://github.com/devpilot-ai"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
