package com.example.bidmartbooking.booking.bootstrap;

import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.example.bidmartbooking.booking.event.WinnerDeterminedPayload;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.dummy-data", havingValue = "true")
public class BookingDummyDataSeeder implements ApplicationRunner {

    private final BookingRepository bookingRepository;
    private final BookingEventConsumer bookingEventConsumer;

    public BookingDummyDataSeeder(
            BookingRepository bookingRepository,
            BookingEventConsumer bookingEventConsumer
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingEventConsumer = bookingEventConsumer;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (bookingRepository.count() > 0) {
            return;
        }
        seedWinnerEvent(
                "evt-seed-001",
                "auc-seed-001",
                "lst-seed-001",
                "usr-seller-01",
                "usr-buyer-01",
                1750000L,
                "Mechanical Keyboard",
                1,
                List.of("usr-buyer-02", "usr-buyer-03")
        );
        seedWinnerEvent(
                "evt-seed-002",
                "auc-seed-002",
                "lst-seed-002",
                "usr-seller-02",
                "usr-buyer-02",
                900000L,
                "Wireless Mouse",
                1,
                List.of("usr-buyer-01")
        );
    }

    private void seedWinnerEvent(
            String eventId,
            String auctionId,
            String listingId,
            String sellerUserId,
            String winnerUserId,
            Long finalPrice,
            String itemName,
            Integer quantity,
            List<String> loserUserIds
    ) {
        EventEnvelope<WinnerDeterminedPayload> event = new EventEnvelope<>();
        event.setEventId(eventId);
        event.setEventType("WinnerDetermined");
        event.setEventVersion(1);
        event.setOccurredAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        event.setSource("bidmart-auction");

        WinnerDeterminedPayload payload = new WinnerDeterminedPayload();
        payload.setAuctionId(auctionId);
        payload.setListingId(listingId);
        payload.setSellerUserId(sellerUserId);
        payload.setWinnerUserId(winnerUserId);
        payload.setFinalPrice(finalPrice);
        payload.setCurrency("IDR");
        payload.setItemName(itemName);
        payload.setQuantity(quantity);
        payload.setLoserUserIds(loserUserIds);

        event.setPayload(payload);
        bookingEventConsumer.handleWinnerDetermined(event);
    }
}
