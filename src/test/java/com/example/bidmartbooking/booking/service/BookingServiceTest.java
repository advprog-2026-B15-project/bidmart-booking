package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Shipment;
import com.example.bidmartbooking.booking.model.ShipmentStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingItemRepository bookingItemRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private BookingStatusAuditLogService auditLogService;

    @Mock
    private RealtimeEventService realtimeEventService;

    @Mock
    private NotificationService notificationService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                bookingItemRepository,
                shipmentRepository,
                auditLogService,
                realtimeEventService,
                notificationService
        );
    }

    @Test
    void shouldReturnMyBookings() {
        Booking booking = new Booking();
        booking.setId(11L);
        when(bookingRepository.findByBuyerUserIdOrderByCreatedAtDesc("usr-1"))
                .thenReturn(List.of(booking));

        List<Booking> result = bookingService.getMyBookings("usr-1");

        assertEquals(1, result.size());
        assertEquals(11L, result.getFirst().getId());
    }

    @Test
    void shouldReturnMySellingBookings() {
        Booking booking = new Booking();
        booking.setId(12L);
        booking.setSellerUserId("seller-1");
        when(bookingRepository.findBySellerUserIdOrderByCreatedAtDesc("seller-1"))
                .thenReturn(List.of(booking));

        List<Booking> result = bookingService.getMySellingBookings("seller-1");

        assertEquals(1, result.size());
        assertEquals(12L, result.getFirst().getId());
    }

    @Test
    void shouldReturnBookingByIdForUser() {
        Booking booking = new Booking();
        booking.setId(10L);
        when(bookingRepository.findByIdAndBuyerUserId(10L, "usr-1"))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingByIdForUser(10L, "usr-1");

        assertEquals(10L, result.getId());
    }

    @Test
    void shouldReturnBookingByIdForSeller() {
        Booking booking = new Booking();
        booking.setId(13L);
        when(bookingRepository.findByIdAndBuyerUserId(13L, "seller-13"))
                .thenReturn(Optional.empty());
        when(bookingRepository.findByIdAndSellerUserId(13L, "seller-13"))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingByIdForUser(13L, "seller-13");

        assertEquals(13L, result.getId());
    }

    @Test
    void shouldThrowWhenBookingNotFoundForUser() {
        when(bookingRepository.findByIdAndBuyerUserId(9L, "usr-404"))
                .thenReturn(Optional.empty());
        when(bookingRepository.findByIdAndSellerUserId(9L, "usr-404"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingByIdForUser(9L, "usr-404")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldReturnExistingBookingWhenEventAlreadyProcessed() {
        Booking existing = new Booking();
        existing.setId(99L);
        existing.setAuctionId("auc-1");
        when(bookingRepository.findBySourceEventId("evt-1")).thenReturn(Optional.of(existing));

        Booking result = bookingService.createBookingFromWinnerEvent(
                "evt-1",
                "auc-1",
                "lst-1",
                "seller-1",
                "buyer-1",
                1000L,
                "IDR",
                "Item",
                1
        );

        assertEquals(99L, result.getId());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void shouldCreateBookingItemAndShipmentFromWinnerEvent() {
        when(bookingRepository.findBySourceEventId("evt-2")).thenReturn(Optional.empty());
        when(bookingRepository.findByAuctionId("auc-2")).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        Booking result = bookingService.createBookingFromWinnerEvent(
                "evt-2",
                "auc-2",
                "lst-2",
                "seller-2",
                "buyer-2",
                2000L,
                "IDR",
                "Keyboard",
                2
        );

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(BookingStatus.CREATED, result.getStatus());

        ArgumentCaptor<BookingItem> itemCaptor = ArgumentCaptor.forClass(BookingItem.class);
        verify(bookingItemRepository).save(itemCaptor.capture());
        BookingItem item = itemCaptor.getValue();
        assertEquals("lst-2", item.getListingId());
        assertEquals("Keyboard", item.getItemName());
        assertEquals(2, item.getQuantity());
        assertEquals(2000L, item.getUnitPrice());
        assertEquals(2000L, item.getSubtotalAmount());

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertEquals(ShipmentStatus.PENDING, shipmentCaptor.getValue().getStatus());
        verify(auditLogService).recordStatusChange(
                result,
                null,
                BookingStatus.CREATED,
                "system",
                "SYSTEM",
                "BOOKING_CREATED_FROM_WINNER_EVENT"
        );
        verify(realtimeEventService).publishBookingStatusChange(
                result,
                null,
                BookingStatus.CREATED,
                "system",
                "SYSTEM",
                "BOOKING_CREATED_FROM_WINNER_EVENT"
        );
    }

    @Test
    void shouldUseFallbackItemNameAndQuantity() {
        when(bookingRepository.findBySourceEventId("evt-3")).thenReturn(Optional.empty());
        when(bookingRepository.findByAuctionId("auc-3")).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(200L);
            return b;
        });

        bookingService.createBookingFromWinnerEvent(
                "evt-3",
                "auc-3",
                "lst-3",
                "seller-3",
                "buyer-3",
                3000L,
                "IDR",
                " ",
                null
        );

        ArgumentCaptor<BookingItem> itemCaptor = ArgumentCaptor.forClass(BookingItem.class);
        verify(bookingItemRepository).save(itemCaptor.capture());
        BookingItem item = itemCaptor.getValue();

        assertEquals("Auction Item", item.getItemName());
        assertEquals(1, item.getQuantity());
    }

    @Test
    void shouldUseFallbackWhenItemNameNullAndQuantityZero() {
        when(bookingRepository.findBySourceEventId("evt-4")).thenReturn(Optional.empty());
        when(bookingRepository.findByAuctionId("auc-4")).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(400L);
            return b;
        });

        bookingService.createBookingFromWinnerEvent(
                "evt-4",
                "auc-4",
                "lst-4",
                "seller-4",
                "buyer-4",
                4000L,
                "IDR",
                null,
                0
        );

        ArgumentCaptor<BookingItem> itemCaptor = ArgumentCaptor.forClass(BookingItem.class);
        verify(bookingItemRepository).save(itemCaptor.capture());
        BookingItem item = itemCaptor.getValue();

        assertEquals("Auction Item", item.getItemName());
        assertEquals(1, item.getQuantity());
    }

    @Test
    void shouldReturnExistingBookingWhenAuctionAlreadyHasBooking() {
        Booking existing = new Booking();
        existing.setId(77L);
        existing.setAuctionId("auc-7");
        when(bookingRepository.findBySourceEventId("evt-7")).thenReturn(Optional.empty());
        when(bookingRepository.findByAuctionId("auc-7")).thenReturn(Optional.of(existing));

        Booking result = bookingService.createBookingFromWinnerEvent(
                "evt-7",
                "auc-7",
                "lst-7",
                "seller-7",
                "buyer-7",
                7000L,
                "IDR",
                "Mouse",
                1
        );

        assertEquals(77L, result.getId());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void shouldTransitionBookingStatusFromCreatedToPaid() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setStatus(BookingStatus.CREATED);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.transitionBookingStatus(500L, BookingStatus.PAID);

        assertEquals(BookingStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(auditLogService).recordStatusChange(
                result,
                BookingStatus.CREATED,
                BookingStatus.PAID,
                "system",
                "SYSTEM",
                "BOOKING_STATUS_TRANSITION"
        );
        verify(realtimeEventService).publishBookingStatusChange(
                result,
                BookingStatus.CREATED,
                BookingStatus.PAID,
                "system",
                "SYSTEM",
                "BOOKING_STATUS_TRANSITION"
        );
    }

    @Test
    void shouldTransitionBookingStatusFromDeliveredToCompleted() {
        Booking booking = new Booking();
        booking.setId(501L);
        booking.setStatus(BookingStatus.DELIVERED);

        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.transitionBookingStatus(501L, BookingStatus.COMPLETED);

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(auditLogService).recordStatusChange(
                result,
                BookingStatus.DELIVERED,
                BookingStatus.COMPLETED,
                "system",
                "SYSTEM",
                "BOOKING_STATUS_TRANSITION"
        );
    }

    @Test
    void shouldRejectInvalidBookingStatusTransition() {
        Booking booking = new Booking();
        booking.setId(502L);
        booking.setStatus(BookingStatus.CREATED);

        when(bookingRepository.findById(502L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.transitionBookingStatus(502L, BookingStatus.SHIPPED)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldThrowWhenTransitionBookingNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.transitionBookingStatus(999L, BookingStatus.PAID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldRejectTransitionWhenCurrentStatusIsNull() {
        Booking booking = new Booking();
        booking.setId(503L);

        when(bookingRepository.findById(503L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.transitionBookingStatus(503L, BookingStatus.PAID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldKeepExistingPaidAtWhenTransitioningToPaid() {
        Booking booking = new Booking();
        OffsetDateTime paidAt = OffsetDateTime.now().minusDays(1);
        booking.setId(504L);
        booking.setStatus(BookingStatus.CREATED);
        booking.setPaidAt(paidAt);

        when(bookingRepository.findById(504L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.transitionBookingStatus(504L, BookingStatus.PAID);

        assertEquals(BookingStatus.PAID, result.getStatus());
        assertEquals(paidAt, result.getPaidAt());
    }

    @Test
    void shouldKeepExistingCompletedAtWhenTransitioningToCompleted() {
        Booking booking = new Booking();
        OffsetDateTime completedAt = OffsetDateTime.now().minusHours(4);
        booking.setId(505L);
        booking.setStatus(BookingStatus.DELIVERED);
        booking.setCompletedAt(completedAt);

        when(bookingRepository.findById(505L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.transitionBookingStatus(505L, BookingStatus.COMPLETED);

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        assertEquals(completedAt, result.getCompletedAt());
    }

    @Test
    void shouldUpdateShipmentToShippedForSeller() {
        Booking booking = new Booking();
        booking.setId(600L);
        booking.setSellerUserId("seller-600");
        booking.setStatus(BookingStatus.PAID);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.PENDING);

        when(bookingRepository.findByIdAndSellerUserId(600L, "seller-600"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(600L)).thenReturn(Optional.of(shipment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = bookingService.updateShipmentForSeller(
                600L,
                "seller-600",
                ShipmentStatus.SHIPPED,
                "JNE-123",
                "JNE"
        );

        assertEquals(ShipmentStatus.SHIPPED, result.getStatus());
        assertEquals("JNE-123", result.getTrackingNumber());
        assertEquals("JNE", result.getCourierName());
        assertNotNull(result.getShippedAt());
        assertEquals(BookingStatus.SHIPPED, booking.getStatus());
        verify(auditLogService).recordStatusChange(
                booking,
                BookingStatus.PAID,
                BookingStatus.SHIPPED,
                "seller-600",
                "SELLER",
                "SHIPMENT_STATUS_UPDATED"
        );
        verify(realtimeEventService).publishBookingStatusChange(
                booking,
                BookingStatus.PAID,
                BookingStatus.SHIPPED,
                "seller-600",
                "SELLER",
                "SHIPMENT_STATUS_UPDATED"
        );
        verify(notificationService).createShippedNotification(any(), any());
    }

    @Test
    void shouldUpdateShipmentToDeliveredForSeller() {
        Booking booking = new Booking();
        booking.setId(601L);
        booking.setSellerUserId("seller-601");
        booking.setStatus(BookingStatus.SHIPPED);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.SHIPPED);

        when(bookingRepository.findByIdAndSellerUserId(601L, "seller-601"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(601L)).thenReturn(Optional.of(shipment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = bookingService.updateShipmentForSeller(
                601L,
                "seller-601",
                ShipmentStatus.DELIVERED,
                null,
                null
        );

        assertEquals(ShipmentStatus.DELIVERED, result.getStatus());
        assertNotNull(result.getDeliveredAt());
        assertEquals(BookingStatus.DELIVERED, booking.getStatus());
        verify(auditLogService).recordStatusChange(
                booking,
                BookingStatus.SHIPPED,
                BookingStatus.DELIVERED,
                "seller-601",
                "SELLER",
                "SHIPMENT_STATUS_UPDATED"
        );
        verify(notificationService).createDeliveredNotification(any(), any());
    }

    @Test
    void shouldRejectInvalidShipmentTransition() {
        Booking booking = new Booking();
        booking.setId(602L);
        booking.setSellerUserId("seller-602");

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.PENDING);

        when(bookingRepository.findByIdAndSellerUserId(602L, "seller-602"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(602L)).thenReturn(Optional.of(shipment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateShipmentForSeller(
                        602L,
                        "seller-602",
                        ShipmentStatus.DELIVERED,
                        null,
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldThrowWhenSellerDoesNotOwnBookingForShipmentUpdate() {
        when(bookingRepository.findByIdAndSellerUserId(603L, "seller-603"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateShipmentForSeller(
                        603L,
                        "seller-603",
                        ShipmentStatus.SHIPPED,
                        "RESI",
                        "JNE"
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowWhenShipmentNotFoundForSellerUpdate() {
        Booking booking = new Booking();
        booking.setId(604L);
        booking.setSellerUserId("seller-604");

        when(bookingRepository.findByIdAndSellerUserId(604L, "seller-604"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(604L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateShipmentForSeller(
                        604L,
                        "seller-604",
                        ShipmentStatus.SHIPPED,
                        "RESI",
                        "JNE"
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldKeepExistingShipmentTrackingMetadataWhenBlank() {
        Booking booking = new Booking();
        booking.setId(605L);
        booking.setSellerUserId("seller-605");
        booking.setStatus(BookingStatus.PAID);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setTrackingNumber("OLD-TRACK");
        shipment.setCourierName("OLD-COURIER");

        when(bookingRepository.findByIdAndSellerUserId(605L, "seller-605"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(605L)).thenReturn(Optional.of(shipment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = bookingService.updateShipmentForSeller(
                605L,
                "seller-605",
                ShipmentStatus.SHIPPED,
                " ",
                ""
        );

        assertEquals("OLD-TRACK", result.getTrackingNumber());
        assertEquals("OLD-COURIER", result.getCourierName());
    }

    @Test
    void shouldRejectShipmentUpdateWhenCurrentStatusIsNull() {
        Booking booking = new Booking();
        booking.setId(606L);
        booking.setSellerUserId("seller-606");

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);

        when(bookingRepository.findByIdAndSellerUserId(606L, "seller-606"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(606L)).thenReturn(Optional.of(shipment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateShipmentForSeller(
                        606L,
                        "seller-606",
                        ShipmentStatus.SHIPPED,
                        "RESI",
                        "JNE"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldKeepExistingShippedAtWhenTransitioningToShipped() {
        Booking booking = new Booking();
        OffsetDateTime shippedAt = OffsetDateTime.now().minusHours(2);
        booking.setId(607L);
        booking.setSellerUserId("seller-607");
        booking.setStatus(BookingStatus.PAID);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setShippedAt(shippedAt);

        when(bookingRepository.findByIdAndSellerUserId(607L, "seller-607"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(607L)).thenReturn(Optional.of(shipment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = bookingService.updateShipmentForSeller(
                607L,
                "seller-607",
                ShipmentStatus.SHIPPED,
                "JNE-607",
                "JNE"
        );

        assertEquals(shippedAt, result.getShippedAt());
        assertEquals(BookingStatus.PAID, booking.getStatus());
    }

    @Test
    void shouldKeepExistingDeliveredAtWhenTransitioningToDelivered() {
        Booking booking = new Booking();
        OffsetDateTime deliveredAt = OffsetDateTime.now().minusHours(3);
        booking.setId(608L);
        booking.setSellerUserId("seller-608");
        booking.setStatus(BookingStatus.SHIPPED);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setDeliveredAt(deliveredAt);

        when(bookingRepository.findByIdAndSellerUserId(608L, "seller-608"))
                .thenReturn(Optional.of(booking));
        when(shipmentRepository.findByBookingId(608L)).thenReturn(Optional.of(shipment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = bookingService.updateShipmentForSeller(
                608L,
                "seller-608",
                ShipmentStatus.DELIVERED,
                null,
                null
        );

        assertEquals(deliveredAt, result.getDeliveredAt());
        assertEquals(BookingStatus.SHIPPED, booking.getStatus());
    }

    @Test
    void shouldConfirmDeliveryForBuyer() {
        Booking booking = new Booking();
        booking.setId(700L);
        booking.setBuyerUserId("buyer-700");
        booking.setStatus(BookingStatus.DELIVERED);

        when(bookingRepository.findByIdAndBuyerUserId(700L, "buyer-700"))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.confirmDeliveryForBuyer(700L, "buyer-700");

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(auditLogService).recordStatusChange(
                result,
                BookingStatus.DELIVERED,
                BookingStatus.COMPLETED,
                "buyer-700",
                "BUYER",
                "BUYER_CONFIRMED_DELIVERY"
        );
        verify(realtimeEventService).publishBookingStatusChange(
                result,
                BookingStatus.DELIVERED,
                BookingStatus.COMPLETED,
                "buyer-700",
                "BUYER",
                "BUYER_CONFIRMED_DELIVERY"
        );
    }

    @Test
    void shouldRejectDeliveryConfirmationWhenBookingNotDelivered() {
        Booking booking = new Booking();
        booking.setId(701L);
        booking.setBuyerUserId("buyer-701");
        booking.setStatus(BookingStatus.SHIPPED);

        when(bookingRepository.findByIdAndBuyerUserId(701L, "buyer-701"))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.confirmDeliveryForBuyer(701L, "buyer-701")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldThrowWhenBuyerDoesNotOwnBookingForDeliveryConfirmation() {
        when(bookingRepository.findByIdAndBuyerUserId(702L, "buyer-702"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.confirmDeliveryForBuyer(702L, "buyer-702")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldKeepExistingCompletedAtWhenBuyerConfirmsDelivery() {
        Booking booking = new Booking();
        OffsetDateTime completedAt = OffsetDateTime.now().minusMinutes(30);
        booking.setId(703L);
        booking.setBuyerUserId("buyer-703");
        booking.setStatus(BookingStatus.DELIVERED);
        booking.setCompletedAt(completedAt);

        when(bookingRepository.findByIdAndBuyerUserId(703L, "buyer-703"))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.confirmDeliveryForBuyer(703L, "buyer-703");

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        assertEquals(completedAt, result.getCompletedAt());
    }
}
