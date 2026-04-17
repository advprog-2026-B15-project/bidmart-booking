package com.example.bidmartbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    void shouldBuildOpenApiMetadata() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.bidmartOpenApi();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Bidmart Booking API", openAPI.getInfo().getTitle());
        assertEquals("v2", openAPI.getInfo().getVersion());
        assertEquals("Bidmart Booking Team", openAPI.getInfo().getContact().getName());
        assertEquals("booking@bidmart.local", openAPI.getInfo().getContact().getEmail());
    }
}
