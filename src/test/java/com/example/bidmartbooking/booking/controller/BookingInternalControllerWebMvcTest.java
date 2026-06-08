package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.NotificationService;
import com.example.bidmartbooking.booking.service.ProcessedEventService;
import com.example.bidmartbooking.booking.service.ReliableEventProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingInternalController.class)
@Import(BookingInternalControllerWebMvcTest.MockConfig.class)
class BookingInternalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingEventConsumer bookingEventConsumer;

    @Test
    void shouldCreateBookingFromWinnerDeterminedEvent() throws Exception {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setAuctionId("auc-10");
        booking.setBuyerUserId("buyer-10");

        doReturn(booking).when(bookingEventConsumer).handleWinnerDetermined(any());

        mockMvc.perform(post("/internal/bookings/winner-determined")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "eventId": "evt-10",
                              "eventType": "WinnerDetermined",
                              "eventVersion": 1,
                              "occurredAt": "2026-06-08T01:00:00Z",
                              "source": "bidmart-auction",
                              "payload": {
                                "auctionId": "auc-10",
                                "listingId": "lst-10",
                                "sellerUserId": "seller-10",
                                "winnerUserId": "buyer-10",
                                "finalPrice": 250000,
                                "currency": "IDR",
                                "itemName": "Jersey",
                                "quantity": 1,
                                "loserUserIds": ["buyer-11"]
                              }
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.bookingId").value(10));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/internal/bookings/winner-determined")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "eventId": "evt-bad",
                              "eventType": "WinnerDetermined",
                              "payload": {
                                "auctionId": "",
                                "listingId": "lst-x"
                              }
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        BookingEventConsumer bookingEventConsumer() {
            BookingService bookingService = mock(BookingService.class);
            NotificationService notificationService = mock(NotificationService.class);
            ProcessedEventService processedEventService = mock(ProcessedEventService.class);
            ReliableEventProcessor reliableEventProcessor = mock(ReliableEventProcessor.class);
            com.example.bidmartbooking.booking.repository.BookingRepository bookingRepository =
                    mock(com.example.bidmartbooking.booking.repository.BookingRepository.class);
            return org.mockito.Mockito.spy(new BookingEventConsumer(
                    bookingService,
                    bookingRepository,
                    notificationService,
                    processedEventService,
                    reliableEventProcessor
            ));
        }
    }
}
