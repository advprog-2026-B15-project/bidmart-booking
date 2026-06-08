package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.example.bidmartbooking.booking.event.WinnerDeterminedPayload;
import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.service.BookingService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/bookings")
public class BookingInternalController {

    private final BookingEventConsumer bookingEventConsumer;
    private final BookingService bookingService;

    public BookingInternalController(BookingEventConsumer bookingEventConsumer, BookingService bookingService) {
        this.bookingEventConsumer = bookingEventConsumer;
        this.bookingService = bookingService;
    }

    @PatchMapping("/pay-by-auction/{auctionId}")
    public Map<String, Object> payByAuction(@PathVariable String auctionId) {
        try {
            Booking booking = bookingService.payBookingByAuctionId(auctionId);
            return Map.of("status", "OK", "bookingId", booking.getId(), "bookingStatus", booking.getStatus().name());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/winner-determined")
    public Map<String, Object> createFromWinnerDetermined(
            @RequestBody EventEnvelope<WinnerDeterminedPayload> event
    ) {
        try {
            Booking booking = bookingEventConsumer.handleWinnerDetermined(event);
            if (booking == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "WinnerDetermined event already processed but booking not found"
                );
            }

            return Map.of(
                    "status", "OK",
                    "eventId", event.getEventId(),
                    "bookingId", booking.getId(),
                    "auctionId", booking.getAuctionId(),
                    "buyerUserId", booking.getBuyerUserId()
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
