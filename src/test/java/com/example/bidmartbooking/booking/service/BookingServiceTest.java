package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Shipment;
import com.example.bidmartbooking.booking.model.ShipmentStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
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

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, bookingItemRepository, shipmentRepository);
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
    void shouldReturnBookingByIdForUser() {
        Booking booking = new Booking();
        booking.setId(10L);
        when(bookingRepository.findByIdAndBuyerUserId(10L, "usr-1"))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingByIdForUser(10L, "usr-1");

        assertEquals(10L, result.getId());
    }

    @Test
    void shouldThrowWhenBookingNotFoundForUser() {
        when(bookingRepository.findByIdAndBuyerUserId(9L, "usr-404"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingByIdForUser(9L, "usr-404")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowConflictWhenEventAlreadyProcessed() {
        when(bookingRepository.existsBySourceEventId("evt-1")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBookingFromWinnerEvent(
                        "evt-1",
                        "auc-1",
                        "lst-1",
                        "seller-1",
                        "buyer-1",
                        1000L,
                        "IDR",
                        "Item",
                        1
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldCreateBookingItemAndShipmentFromWinnerEvent() {
        when(bookingRepository.existsBySourceEventId("evt-2")).thenReturn(false);
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
    }

    @Test
    void shouldUseFallbackItemNameAndQuantity() {
        when(bookingRepository.existsBySourceEventId("evt-3")).thenReturn(false);
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
        when(bookingRepository.existsBySourceEventId("evt-4")).thenReturn(false);
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
}
