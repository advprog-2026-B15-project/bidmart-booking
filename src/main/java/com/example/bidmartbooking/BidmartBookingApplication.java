package com.example.bidmartbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BidmartBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BidmartBookingApplication.class, args);
    }

}
