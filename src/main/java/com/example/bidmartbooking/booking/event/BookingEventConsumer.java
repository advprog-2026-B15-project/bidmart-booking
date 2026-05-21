package com.example.bidmartbooking.booking.event;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.NotificationService;
import com.example.bidmartbooking.booking.service.ProcessedEventService;
import com.example.bidmartbooking.booking.service.ReliableEventProcessor;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingEventConsumer {

    private static final String WINNER_DETERMINED = "WinnerDetermined";
    private static final String AUCTION_CLOSED = "AuctionClosed";
    private static final String BID_PLACED = "BidPlaced";
    private static final String BALANCE_CONVERTED = "BalanceConverted";
    private static final String BALANCE_RELEASED = "BalanceReleased";

    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final ProcessedEventService processedEventService;
    private final ReliableEventProcessor reliableEventProcessor;

    public BookingEventConsumer(
            BookingService bookingService,
            NotificationService notificationService,
            ProcessedEventService processedEventService,
            ReliableEventProcessor reliableEventProcessor
    ) {
        this.bookingService = bookingService;
        this.notificationService = notificationService;
        this.processedEventService = processedEventService;
        this.reliableEventProcessor = reliableEventProcessor;
    }

    public Booking handleWinnerDetermined(EventEnvelope<WinnerDeterminedPayload> event) {
        validateWinnerEvent(event);
        if (hasAlreadyProcessed(event)) {
            return null;
        }

        return reliableEventProcessor.process(event, WINNER_DETERMINED, () -> {
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

            processedEventService.markProcessed(event.getEventId(), WINNER_DETERMINED);
            return booking;
        });
    }

    public void handleAuctionClosed(EventEnvelope<AuctionClosedPayload> event) {
        validateAuctionClosedEvent(event);
        if (hasAlreadyProcessed(event)) {
            return;
        }
        reliableEventProcessor.process(event, AUCTION_CLOSED, () -> {
            processedEventService.markProcessed(event.getEventId(), AUCTION_CLOSED);
            return null;
        });
    }

    public void handleBidPlaced(EventEnvelope<BidPlacedPayload> event) {
        validateBidPlacedEvent(event);
        if (hasAlreadyProcessed(event)) {
            return;
        }

        reliableEventProcessor.process(event, BID_PLACED, () -> {
            BidPlacedPayload payload = event.getPayload();
            notificationService.createBidPlacedNotifications(
                    payload.getSellerUserId(),
                    payload.getBidderUserId(),
                    payload.getPreviousHighestBidderUserId(),
                    payload.getAuctionId(),
                    payload.getBidAmount(),
                    payload.getItemName()
            );
            processedEventService.markProcessed(event.getEventId(), BID_PLACED);
            return null;
        });
    }

    public void handleBalanceConverted(EventEnvelope<BalanceConvertedPayload> event) {
        validateBalanceConvertedEvent(event);
        if (hasAlreadyProcessed(event)) {
            return;
        }

        reliableEventProcessor.process(event, BALANCE_CONVERTED, () -> {
            BalanceConvertedPayload payload = event.getPayload();
            notificationService.createBalanceConvertedNotification(
                    payload.getUserId(),
                    payload.getAuctionId(),
                    payload.getAmount()
            );
            if (payload.getBookingId() != null && !payload.getBookingId().isBlank()) {
                bookingService.transitionBookingStatus(
                        Long.parseLong(payload.getBookingId()), BookingStatus.PAID
                );
            }
            processedEventService.markProcessed(event.getEventId(), BALANCE_CONVERTED);
            return null;
        });
    }

    public void handleBalanceReleased(EventEnvelope<BalanceReleasedPayload> event) {
        validateBalanceReleasedEvent(event);
        if (hasAlreadyProcessed(event)) {
            return;
        }

        reliableEventProcessor.process(event, BALANCE_RELEASED, () -> {
            BalanceReleasedPayload payload = event.getPayload();
            notificationService.createBalanceReleasedNotification(
                    payload.getUserId(),
                    payload.getAuctionId(),
                    payload.getAmount()
            );
            processedEventService.markProcessed(event.getEventId(), BALANCE_RELEASED);
            return null;
        });
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
        if (Boolean.TRUE.equals(payload.getHasWinner())) {
            requireNonBlank(
                    payload.getWinnerUserId(),
                    "winnerUserId is required when hasWinner=true"
            );
        }
    }

    private void validateBidPlacedEvent(EventEnvelope<BidPlacedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new IllegalArgumentException("Invalid event: payload is required");
        }

        BidPlacedPayload payload = event.getPayload();

        requireNonBlank(event.getEventId(), "eventId is required");
        requireNonBlank(payload.getAuctionId(), "auctionId is required");
        requireNonBlank(payload.getListingId(), "listingId is required");
        requireNonBlank(payload.getSellerUserId(), "sellerUserId is required");
        requireNonBlank(payload.getBidderUserId(), "bidderUserId is required");
        if (payload.getBidAmount() == null || payload.getBidAmount() < 0) {
            throw new IllegalArgumentException("bidAmount must be >= 0");
        }
    }

    private void validateBalanceConvertedEvent(EventEnvelope<BalanceConvertedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new IllegalArgumentException("Invalid event: payload is required");
        }

        BalanceConvertedPayload payload = event.getPayload();

        requireNonBlank(event.getEventId(), "eventId is required");
        requireNonBlank(payload.getUserId(), "userId is required");
        if (payload.getAmount() == null || payload.getAmount() < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
    }

    private void validateBalanceReleasedEvent(EventEnvelope<BalanceReleasedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new IllegalArgumentException("Invalid event: payload is required");
        }

        BalanceReleasedPayload payload = event.getPayload();

        requireNonBlank(event.getEventId(), "eventId is required");
        requireNonBlank(payload.getUserId(), "userId is required");
        if (payload.getAmount() == null || payload.getAmount() < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean hasAlreadyProcessed(EventEnvelope<?> event) {
        return processedEventService.hasProcessed(event.getEventId());
    }
}
