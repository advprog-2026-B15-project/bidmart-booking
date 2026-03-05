package com.example.bidmartbooking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BidmartBookingApplicationMainTest {

    @Test
    void mainShouldThrowForInvalidDatasourceConfig() {
        assertThrows(Exception.class, () -> BidmartBookingApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.flyway.enabled=false",
                "--spring.datasource.url=jdbc:invalid",
                "--spring.datasource.driver-class-name=invalid.Driver",
                "--app.seed.dummy-data=false"
        }));
    }
}
