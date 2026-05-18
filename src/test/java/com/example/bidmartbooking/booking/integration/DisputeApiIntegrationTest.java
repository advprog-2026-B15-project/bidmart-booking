package com.example.bidmartbooking.booking.integration;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Dispute;
import com.example.bidmartbooking.booking.model.DisputeStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.DisputeRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DisputeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @BeforeEach
    void setUp() {
        disputeRepository.deleteAll();
        shipmentRepository.deleteAll();
        bookingItemRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void shouldFileDisputeSuccessfullyForDeliveredBooking() throws Exception {
        Booking booking = createBooking("auc-d1", "buyer-d1", "seller-d1", BookingStatus.DELIVERED);

        mockMvc.perform(post("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "buyer-d1")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai dengan deskripsi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.filedByUserId").value("buyer-d1"))
                .andExpect(jsonPath("$.bookingId").value(booking.getId()));

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.DISPUTED, updated.getStatus());
    }

    @Test
    void shouldRejectDisputeWhenBookingNotDelivered() throws Exception {
        Booking booking = createBooking("auc-d2", "buyer-d2", "seller-d2", BookingStatus.SHIPPED);

        mockMvc.perform(post("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "buyer-d2")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai dengan deskripsi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDisputeWhenReasonTooShort() throws Exception {
        Booking booking = createBooking("auc-d3", "buyer-d3", "seller-d3", BookingStatus.DELIVERED);

        mockMvc.perform(post("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "buyer-d3")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"pendek\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDisputeWhenRoleIsNotBuyer() throws Exception {
        Booking booking = createBooking("auc-d4", "buyer-d4", "seller-d4", BookingStatus.DELIVERED);

        mockMvc.perform(post("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "seller-d4")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai dengan deskripsi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateDispute() throws Exception {
        Booking booking = createBooking("auc-d5", "buyer-d5", "seller-d5", BookingStatus.DELIVERED);

        mockMvc.perform(post("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "buyer-d5")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai dengan deskripsi\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "buyer-d5")
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Barang tidak sesuai dengan deskripsi lagi\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetDisputeAsBuyer() throws Exception {
        Booking booking = createBooking("auc-d6", "buyer-d6", "seller-d6", BookingStatus.DISPUTED);
        createDisputeFor(booking, "buyer-d6");

        mockMvc.perform(get("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "buyer-d6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filedByUserId").value("buyer-d6"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldGetDisputeAsSeller() throws Exception {
        Booking booking = createBooking("auc-d7", "buyer-d7", "seller-d7", BookingStatus.DISPUTED);
        createDisputeFor(booking, "buyer-d7");

        mockMvc.perform(get("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "seller-d7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturn403WhenUnrelatedUserGetsDispute() throws Exception {
        Booking booking = createBooking("auc-d8", "buyer-d8", "seller-d8", BookingStatus.DISPUTED);
        createDisputeFor(booking, "buyer-d8");

        mockMvc.perform(get("/api/bookings/{id}/dispute", booking.getId())
                        .header("X-User-Id", "stranger"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetMySellingBookings() throws Exception {
        createBooking("auc-s1", "buyer-s1", "seller-x", BookingStatus.CREATED);
        createBooking("auc-s2", "buyer-s2", "seller-x", BookingStatus.SHIPPED);
        createBooking("auc-s3", "buyer-s3", "other-seller", BookingStatus.CREATED);

        mockMvc.perform(get("/api/bookings/selling")
                        .header("X-User-Id", "seller-x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnEmptyListWhenSellerHasNoBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/selling")
                        .header("X-User-Id", "unknown-seller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Booking createBooking(String auctionId, String buyer, String seller, BookingStatus status) {
        Booking booking = new Booking();
        booking.setSourceEventId("evt-" + auctionId);
        booking.setAuctionId(auctionId);
        booking.setListingId("lst-" + auctionId);
        booking.setBuyerUserId(buyer);
        booking.setSellerUserId(seller);
        booking.setStatus(status);
        booking.setTotalAmount(100000L);
        booking.setCurrency("IDR");
        return bookingRepository.save(booking);
    }

    private void createDisputeFor(Booking booking, String filedByUserId) {
        Dispute dispute = new Dispute();
        dispute.setBooking(booking);
        dispute.setFiledByUserId(filedByUserId);
        dispute.setReason("Barang tidak sesuai deskripsi");
        dispute.setStatus(DisputeStatus.OPEN);
        disputeRepository.save(dispute);
    }
}
