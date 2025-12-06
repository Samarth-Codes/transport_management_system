package com.cargopro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration
 * 
 * This class configures Swagger UI documentation for the API
 * After adding this, Swagger UI will be available at:
 * http://localhost:8080/swagger-ui/index.html
 * 
 * API documentation (JSON) will be available at:
 * http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transportManagementSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transport Management System API")
                        .description("REST API for managing loads, transporters, bids, and bookings")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CargoPro Team")
                                .email("support@cargopro.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

