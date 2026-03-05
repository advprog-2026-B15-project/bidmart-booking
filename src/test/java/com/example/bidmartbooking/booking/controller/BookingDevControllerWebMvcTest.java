package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.NotificationService;
import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingDevController.class)
@Import(BookingDevControllerWebMvcTest.MockConfig.class)
class BookingDevControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingEventConsumer bookingEventConsumer;

    @Test
    void shouldSimulateWinnerDeterminedEvent() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setAuctionId("auc-x");
        booking.setBuyerUserId("usr-buyer-1");

        doReturn(booking).when(bookingEventConsumer).handleWinnerDetermined(any());

        mockMvc.perform(post("/api/dev/events/winner-determined")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "auctionId": "auc-x",
                              "listingId": "lst-x",
                              "sellerUserId": "usr-seller-1",
                              "winnerUserId": "usr-buyer-1",
                              "finalPrice": 1200000,
                              "currency": "IDR",
                              "itemName": "Headset",
                              "quantity": 1,
                              "loserUserIds": ["usr-buyer-2"]
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.bookingId").value(1));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/dev/events/winner-determined")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "listingId": "lst-x",
                              "sellerUserId": "usr-seller-1",
                              "winnerUserId": "usr-buyer-1",
                              "finalPrice": 1200000,
                              "itemName": "Headset",
                              "quantity": 1
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptRequestWhenCurrencyIsNull() throws Exception {
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setAuctionId("auc-y");
        booking.setBuyerUserId("usr-buyer-1");
        doReturn(booking).when(bookingEventConsumer).handleWinnerDetermined(any());

        mockMvc.perform(post("/api/dev/events/winner-determined")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "auctionId": "auc-y",
                              "listingId": "lst-y",
                              "sellerUserId": "usr-seller-1",
                              "winnerUserId": "usr-buyer-1",
                              "finalPrice": 1300000,
                              "itemName": "Keyboard",
                              "quantity": 1
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(2));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        BookingEventConsumer bookingEventConsumer() {
            BookingService bookingService = mock(BookingService.class);
            NotificationService notificationService = mock(NotificationService.class);
            return org.mockito.Mockito.spy(new BookingEventConsumer(bookingService, notificationService));
        }
    }
}
