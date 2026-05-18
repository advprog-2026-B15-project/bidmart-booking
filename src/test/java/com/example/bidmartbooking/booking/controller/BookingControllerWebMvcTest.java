package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Dispute;
import com.example.bidmartbooking.booking.model.DisputeStatus;
import com.example.bidmartbooking.booking.model.Shipment;
import com.example.bidmartbooking.booking.model.ShipmentStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.DisputeService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockBean
    private DisputeService disputeService;

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

    @Test
    void shouldUpdateShipmentForSeller() throws Exception {
        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setTrackingNumber("RESI-1");
        shipment.setCourierName("JNE");

        when(bookingService.updateShipmentForSeller(
                4L,
                "seller-4",
                ShipmentStatus.SHIPPED,
                "RESI-1",
                "JNE"
        )).thenReturn(shipment);

        mockMvc.perform(patch("/api/bookings/4/shipment")
                        .header("X-User-Id", "seller-4")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\",\"trackingNumber\":\"RESI-1\",\"courierName\":\"JNE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(4))
                .andExpect(jsonPath("$.shipmentStatus").value("SHIPPED"));
    }

    @Test
    void shouldRejectShipmentUpdateWhenRoleIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/bookings/4/shipment")
                        .header("X-User-Id", "buyer-4")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldConfirmDeliveryForBuyer() throws Exception {
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingService.confirmDeliveryForBuyer(5L, "buyer-5")).thenReturn(booking);

        mockMvc.perform(patch("/api/bookings/5/confirm-delivery")
                        .header("X-User-Id", "buyer-5")
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(5))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldRejectConfirmDeliveryWhenRoleIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/bookings/5/confirm-delivery")
                        .header("X-User-Id", "seller-5")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetMySellingBookings() throws Exception {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setAuctionId("auc-10");
        booking.setListingId("lst-10");
        booking.setBuyerUserId("buyer-10");
        booking.setSellerUserId("seller-10");
        booking.setStatus(BookingStatus.SHIPPED);
        booking.setTotalAmount(200000L);
        booking.setCurrency("IDR");
        booking.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(bookingService.getMySellingBookings("seller-10")).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings/selling").header("X-User-Id", "seller-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].sellerUserId").value("seller-10"))
                .andExpect(jsonPath("$[0].status").value("SHIPPED"));
    }

    @Test
    void shouldFileDisputeAsBuyer() throws Exception {
        Booking booking = new Booking();
        booking.setId(20L);
        booking.setBuyerUserId("buyer-20");
        booking.setSellerUserId("seller-20");
        booking.setStatus(BookingStatus.DISPUTED);

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setBooking(booking);
        dispute.setFiledByUserId("buyer-20");
        dispute.setReason("Barang tidak sesuai deskripsi produk");
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        dispute.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(disputeService.fileDispute(20L, "buyer-20", "Barang tidak sesuai deskripsi produk"))
                .thenReturn(dispute);

        mockMvc.perform(post("/api/bookings/20/dispute")
                        .header("X-User-Id", "buyer-20")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai deskripsi produk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filedByUserId").value("buyer-20"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldRejectDisputeWhenRoleIsNotBuyer() throws Exception {
        mockMvc.perform(post("/api/bookings/20/dispute")
                        .header("X-User-Id", "seller-20")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai deskripsi produk\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetDisputeForBooking() throws Exception {
        Booking booking = new Booking();
        booking.setId(21L);
        booking.setBuyerUserId("buyer-21");
        booking.setSellerUserId("seller-21");
        booking.setStatus(BookingStatus.DISPUTED);

        Dispute dispute = new Dispute();
        dispute.setId(2L);
        dispute.setBooking(booking);
        dispute.setFiledByUserId("buyer-21");
        dispute.setReason("Produk cacat saat tiba");
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        dispute.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(disputeService.getDisputeForBooking(21L, "buyer-21")).thenReturn(dispute);

        mockMvc.perform(get("/api/bookings/21/dispute")
                        .header("X-User-Id", "buyer-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.reason").value("Produk cacat saat tiba"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturn403WhenGetDisputeForbidden() throws Exception {
        when(disputeService.getDisputeForBooking(anyLong(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/bookings/22/dispute")
                        .header("X-User-Id", "stranger"))
                .andExpect(status().isForbidden());
    }
}
