package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Shipment;
import com.example.bidmartbooking.booking.model.ShipmentStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import com.example.bidmartbooking.booking.service.BookingService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingItemRepository bookingItemRepository;

    @MockBean
    private ShipmentRepository shipmentRepository;

    @Test
    void shouldGetMyBookings() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setAuctionId("auc-1");
        booking.setListingId("lst-1");
        booking.setBuyerUserId("usr-1");
        booking.setSellerUserId("seller-1");
        booking.setStatus(BookingStatus.CREATED);
        booking.setTotalAmount(123000L);
        booking.setCurrency("IDR");
        booking.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(bookingService.getMyBookings("usr-1")).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings/me").header("X-User-Id", "usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].auctionId").value("auc-1"))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    void shouldGetBookingDetailWithShipment() throws Exception {
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setAuctionId("auc-2");
        booking.setListingId("lst-2");
        booking.setBuyerUserId("usr-1");
        booking.setSellerUserId("seller-2");
        booking.setStatus(BookingStatus.SHIPPED);
        booking.setTotalAmount(500000L);
        booking.setCurrency("IDR");
        booking.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        booking.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        BookingItem item = new BookingItem();
        item.setListingId("lst-2");
        item.setItemName("Mouse");
        item.setQuantity(1);
        item.setUnitPrice(500000L);
        item.setSubtotalAmount(500000L);

        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setTrackingNumber("TRX-99");
        shipment.setCourierName("JNE");

        when(bookingService.getBookingByIdForUser(2L, "usr-1")).thenReturn(booking);
        when(bookingItemRepository.findByBookingId(2L)).thenReturn(List.of(item));
        when(shipmentRepository.findByBookingId(2L)).thenReturn(Optional.of(shipment));

        mockMvc.perform(get("/api/bookings/2").header("X-User-Id", "usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.items[0].itemName").value("Mouse"))
                .andExpect(jsonPath("$.shipment.status").value("SHIPPED"));
    }

    @Test
    void shouldGetBookingDetailWithoutShipment() throws Exception {
        Booking booking = new Booking();
        booking.setId(3L);
        booking.setAuctionId("auc-3");
        booking.setListingId("lst-3");
        booking.setBuyerUserId("usr-1");
        booking.setSellerUserId("seller-3");
        booking.setStatus(BookingStatus.CREATED);
        booking.setTotalAmount(100000L);
        booking.setCurrency("IDR");
        booking.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        booking.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(bookingService.getBookingByIdForUser(3L, "usr-1")).thenReturn(booking);
        when(bookingItemRepository.findByBookingId(3L)).thenReturn(List.of());
        when(shipmentRepository.findByBookingId(3L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bookings/3").header("X-User-Id", "usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundWhenBookingNotOwned() throws Exception {
        when(bookingService.getBookingByIdForUser(99L, "usr-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        mockMvc.perform(get("/api/bookings/99").header("X-User-Id", "usr-1"))
                .andExpect(status().isNotFound());
    }
}
