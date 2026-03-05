package com.example.bidmartbooking.booking.event;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.NotificationService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingEventConsumer {

    private final BookingService bookingService;
    private final NotificationService notificationService;

    public BookingEventConsumer(
            BookingService bookingService,
            NotificationService notificationService
    ) {
        this.bookingService = bookingService;
        this.notificationService = notificationService;
    }

    public Booking handleWinnerDetermined(EventEnvelope<WinnerDeterminedPayload> event) {
        validateWinnerEvent(event);

        WinnerDeterminedPayload payload = event.getPayload();
        String currency = payload.getCurrency() != null ? payload.getCurrency() : "IDR";

        Booking booking = bookingService.createBookingFromWinnerEvent(
                event.getEventId(),
                payload.getAuctionId(),
                payload.getListingId(),
                payload.getSellerUserId(),
                payload.getWinnerUserId(),
                payload.getFinalPrice(),
                currency,
                payload.getItemName(),
                payload.getQuantity()
        );

        List<String> loserUserIds = payload.getLoserUserIds() != null
                ? payload.getLoserUserIds()
                : Collections.emptyList();

        notificationService.createWinLoseNotifications(
                payload.getWinnerUserId(),
                loserUserIds,
                payload.getAuctionId(),
                payload.getFinalPrice()
        );

        return booking;
    }

    public void handleAuctionClosed(EventEnvelope<AuctionClosedPayload> event) {
        validateAuctionClosedEvent(event);
        AuctionClosedPayload payload = event.getPayload();
        if (Boolean.TRUE.equals(payload.getHasWinner())) {
            requireNonBlank(payload.getWinnerUserId(), "winnerUserId is required when hasWinner=true");
        }
    }

    private void validateWinnerEvent(EventEnvelope<WinnerDeterminedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new IllegalArgumentException("Invalid event: payload is required");
        }

        WinnerDeterminedPayload payload = event.getPayload();

        requireNonBlank(event.getEventId(), "eventId is required");
        requireNonBlank(payload.getAuctionId(), "auctionId is required");
        requireNonBlank(payload.getListingId(), "listingId is required");
        requireNonBlank(payload.getSellerUserId(), "sellerUserId is required");
        requireNonBlank(payload.getWinnerUserId(), "winnerUserId is required");

        if (payload.getFinalPrice() == null || payload.getFinalPrice() < 0) {
            throw new IllegalArgumentException("finalPrice must be >= 0");
        }
    }

    private void validateAuctionClosedEvent(EventEnvelope<AuctionClosedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new IllegalArgumentException("Invalid event: payload is required");
        }

        AuctionClosedPayload payload = event.getPayload();

        requireNonBlank(event.getEventId(), "eventId is required");
        requireNonBlank(payload.getAuctionId(), "auctionId is required");
        requireNonBlank(payload.getListingId(), "listingId is required");
        if (payload.getHasWinner() == null) {
            throw new IllegalArgumentException("hasWinner is required");
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
