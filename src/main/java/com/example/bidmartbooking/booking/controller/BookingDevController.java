package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.dto.TriggerWinnerEventRequest;
import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.example.bidmartbooking.booking.event.WinnerDeterminedPayload;
import com.example.bidmartbooking.booking.model.Booking;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/events")
public class BookingDevController {

    private final BookingEventConsumer bookingEventConsumer;

    public BookingDevController(BookingEventConsumer bookingEventConsumer) {
        this.bookingEventConsumer = bookingEventConsumer;
    }

    @PostMapping("/winner-determined")
    public Map<String, Object> simulateWinnerDetermined(
            @Valid @RequestBody TriggerWinnerEventRequest request
    ) {
        EventEnvelope<WinnerDeterminedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-sim-" + UUID.randomUUID());
        event.setEventType("WinnerDetermined");
        event.setEventVersion(1);
        event.setOccurredAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        event.setSource("bidmart-auction-simulator");

        WinnerDeterminedPayload payload = new WinnerDeterminedPayload();
        payload.setAuctionId(request.getAuctionId());
        payload.setListingId(request.getListingId());
        payload.setSellerUserId(request.getSellerUserId());
        payload.setWinnerUserId(request.getWinnerUserId());
        payload.setFinalPrice(request.getFinalPrice());
        payload.setCurrency(request.getCurrency() == null ? "IDR" : request.getCurrency());
        payload.setItemName(request.getItemName());
        payload.setQuantity(request.getQuantity());

        List<String> loserUserIds = request.getLoserUserIds();
        payload.setLoserUserIds(loserUserIds);
        event.setPayload(payload);

        Booking booking = bookingEventConsumer.handleWinnerDetermined(event);

        return Map.of(
                "status", "OK",
                "eventId", event.getEventId(),
                "bookingId", booking.getId(),
                "auctionId", booking.getAuctionId(),
                "buyerUserId", booking.getBuyerUserId()
        );
    }
}
