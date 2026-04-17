package com.example.bidmartbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bidmartOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bidmart Booking API")
                        .description("OpenAPI specification for booking and notification endpoints")
                        .version("v2")
                        .contact(new Contact()
                                .name("Bidmart Booking Team")
                                .email("booking@bidmart.local")));
    }
}
