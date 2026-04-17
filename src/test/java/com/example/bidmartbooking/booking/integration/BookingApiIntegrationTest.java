package com.example.bidmartbooking.booking.integration;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Shipment;
import com.example.bidmartbooking.booking.model.ShipmentStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
        bookingItemRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void shouldGetBookingsForCurrentUser() throws Exception {
        createBooking("auc-1", "usr-a", "seller-a", 100000L);
        createBooking("auc-2", "usr-b", "seller-b", 200000L);

        mockMvc.perform(get("/api/bookings/me").header("X-User-Id", "usr-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].buyerUserId").value("usr-a"))
                .andExpect(jsonPath("$[0].auctionId").value("auc-1"));
    }

    @Test
    void shouldGetBookingDetailWithItemsAndShipment() throws Exception {
        Booking booking = createBooking("auc-3", "usr-a", "seller-a", 300000L);

        BookingItem item = new BookingItem();
        item.setBooking(booking);
        item.setListingId("lst-3");
        item.setItemName("Keyboard");
        item.setQuantity(1);
        item.setUnitPrice(300000L);
        item.setSubtotalAmount(300000L);
        bookingItemRepository.save(item);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setTrackingNumber("RESI-001");
        shipment.setCourierName("JNE");
        shipmentRepository.save(shipment);

        mockMvc.perform(get("/api/bookings/{id}", booking.getId()).header("X-User-Id", "usr-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.items[0].itemName").value("Keyboard"))
                .andExpect(jsonPath("$.shipment.status").value("SHIPPED"));
    }

    @Test
    void shouldReturnNotFoundWhenUserAccessesForeignBooking() throws Exception {
        Booking booking = createBooking("auc-4", "usr-owner", "seller-x", 400000L);

        mockMvc.perform(get("/api/bookings/{id}", booking.getId()).header("X-User-Id", "usr-other"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldUpdateShipmentForSeller() throws Exception {
        Booking booking = createBooking("auc-5", "buyer-5", "seller-5", 500000L);
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipmentRepository.save(shipment);

        mockMvc.perform(patch("/api/bookings/{id}/shipment", booking.getId())
                        .header("X-User-Id", "seller-5")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\",\"trackingNumber\":\"RESI-5\",\"courierName\":\"JNE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingNumber").value("RESI-5"));
    }

    @Test
    void shouldConfirmDeliveryForBuyer() throws Exception {
        Booking booking = createBooking("auc-6", "buyer-6", "seller-6", 600000L);
        booking.setStatus(BookingStatus.DELIVERED);
        bookingRepository.save(booking);

        mockMvc.perform(patch("/api/bookings/{id}/confirm-delivery", booking.getId())
                        .header("X-User-Id", "buyer-6")
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private Booking createBooking(String auctionId, String buyer, String seller, Long total) {
        Booking booking = new Booking();
        booking.setSourceEventId("evt-" + auctionId);
        booking.setAuctionId(auctionId);
        booking.setListingId("lst-" + auctionId);
        booking.setBuyerUserId(buyer);
        booking.setSellerUserId(seller);
        booking.setStatus(BookingStatus.CREATED);
        booking.setTotalAmount(total);
        booking.setCurrency("IDR");
        return bookingRepository.save(booking);
    }
}
