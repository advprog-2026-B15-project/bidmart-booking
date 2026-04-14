package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.dto.BookingDetailResponse;
import com.example.bidmartbooking.booking.dto.BookingItemResponse;
import com.example.bidmartbooking.booking.dto.BookingSummaryResponse;
import com.example.bidmartbooking.booking.dto.DeliveryConfirmationResponse;
import com.example.bidmartbooking.booking.dto.ShipmentResponse;
import com.example.bidmartbooking.booking.dto.ShipmentUpdateResponse;
import com.example.bidmartbooking.booking.dto.UpdateShipmentRequest;
import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.Shipment;
import jakarta.validation.Valid;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import com.example.bidmartbooking.booking.service.BookingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingItemRepository bookingItemRepository;
    private final ShipmentRepository shipmentRepository;

    public BookingController(
            BookingService bookingService,
            BookingItemRepository bookingItemRepository,
            ShipmentRepository shipmentRepository
    ) {
        this.bookingService = bookingService;
        this.bookingItemRepository = bookingItemRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping("/me")
    public List<BookingSummaryResponse> getMyBookings(
            @RequestHeader("X-User-Id") String userId
    ) {
        return bookingService.getMyBookings(userId)
                .stream()
                .map(this::toBookingSummaryResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public BookingDetailResponse getBookingById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId
    ) {
        Booking booking = bookingService.getBookingByIdForUser(id, userId);
        List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());
        Shipment shipment = shipmentRepository.findByBookingId(booking.getId()).orElse(null);

        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(booking.getId());
        response.setAuctionId(booking.getAuctionId());
        response.setListingId(booking.getListingId());
        response.setBuyerUserId(booking.getBuyerUserId());
        response.setSellerUserId(booking.getSellerUserId());
        response.setStatus(booking.getStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setCurrency(booking.getCurrency());
        response.setItems(items.stream().map(this::toBookingItemResponse).toList());
        response.setShipment(toShipmentResponse(shipment));
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }

    @PatchMapping("/{id}/shipment")
    public ShipmentUpdateResponse updateShipment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody UpdateShipmentRequest request
    ) {
        enforceRole(userRole, "SELLER");
        Shipment shipment = bookingService.updateShipmentForSeller(
                id,
                userId,
                request.getStatus(),
                request.getTrackingNumber(),
                request.getCourierName()
        );

        ShipmentUpdateResponse response = new ShipmentUpdateResponse();
        response.setBookingId(id);
        response.setShipmentStatus(shipment.getStatus());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setCourierName(shipment.getCourierName());
        return response;
    }

    @PatchMapping("/{id}/confirm-delivery")
    public DeliveryConfirmationResponse confirmDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole
    ) {
        enforceRole(userRole, "BUYER");
        Booking booking = bookingService.confirmDeliveryForBuyer(id, userId);

        DeliveryConfirmationResponse response = new DeliveryConfirmationResponse();
        response.setBookingId(booking.getId());
        response.setStatus(booking.getStatus());
        return response;
    }

    private BookingSummaryResponse toBookingSummaryResponse(Booking booking) {
        BookingSummaryResponse response = new BookingSummaryResponse();
        response.setId(booking.getId());
        response.setAuctionId(booking.getAuctionId());
        response.setListingId(booking.getListingId());
        response.setBuyerUserId(booking.getBuyerUserId());
        response.setSellerUserId(booking.getSellerUserId());
        response.setStatus(booking.getStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setCurrency(booking.getCurrency());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    private BookingItemResponse toBookingItemResponse(BookingItem item) {
        BookingItemResponse response = new BookingItemResponse();
        response.setListingId(item.getListingId());
        response.setItemName(item.getItemName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotalAmount(item.getSubtotalAmount());
        return response;
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        ShipmentResponse response = new ShipmentResponse();
        response.setStatus(shipment.getStatus());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setCourierName(shipment.getCourierName());
        response.setShippedAt(shipment.getShippedAt());
        response.setDeliveredAt(shipment.getDeliveredAt());
        return response;
    }

    private void enforceRole(String actualRole, String expectedRole) {
        if (!expectedRole.equalsIgnoreCase(actualRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid user role");
        }
    }
}
