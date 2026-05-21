package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.dto.BookingDetailResponse;
import com.example.bidmartbooking.booking.dto.BookingItemResponse;
import com.example.bidmartbooking.booking.dto.BookingSummaryResponse;
import com.example.bidmartbooking.booking.dto.DeliveryConfirmationResponse;
import com.example.bidmartbooking.booking.dto.DisputeRequest;
import com.example.bidmartbooking.booking.dto.DisputeResponse;
import com.example.bidmartbooking.booking.dto.ShipmentResponse;
import com.example.bidmartbooking.booking.dto.ShipmentUpdateResponse;
import com.example.bidmartbooking.booking.dto.UpdateShipmentRequest;
import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.Dispute;
import com.example.bidmartbooking.booking.model.Shipment;
import jakarta.validation.Valid;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Booking lifecycle and shipment management endpoints")
public class BookingController {

    private final BookingService bookingService;
    private final BookingItemRepository bookingItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final DisputeService disputeService;

    public BookingController(
            BookingService bookingService,
            BookingItemRepository bookingItemRepository,
            ShipmentRepository shipmentRepository,
            DisputeService disputeService
    ) {
        this.bookingService = bookingService;
        this.bookingItemRepository = bookingItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.disputeService = disputeService;
    }

    @GetMapping("/me")
    @Operation(summary = "List my bookings as buyer")
    public List<BookingSummaryResponse> getMyBookings(
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId
    ) {
        return bookingService.getMyBookings(userId)
                .stream()
                .map(this::toBookingSummaryResponse)
                .toList();
    }

    @GetMapping("/selling")
    @Operation(summary = "List bookings where I am the seller")
    public List<BookingSummaryResponse> getMySellingBookings(
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId
    ) {
        return bookingService.getMySellingBookings(userId)
                .stream()
                .map(this::toBookingSummaryResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking detail for current buyer")
    public BookingDetailResponse getBookingById(
            @Parameter(description = "Booking id", required = true)
            @PathVariable Long id,
            @Parameter(description = "Current user id", required = true)
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
    @Operation(summary = "Update shipment status as seller")
    public ShipmentUpdateResponse updateShipment(
            @Parameter(description = "Booking id", required = true)
            @PathVariable Long id,
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Current user role, must be SELLER", required = true)
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

    @PostMapping("/{id}/dispute")
    @Operation(summary = "File a dispute as buyer after delivery")
    public DisputeResponse fileDispute(
            @Parameter(description = "Booking id", required = true)
            @PathVariable Long id,
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Current user role, must be BUYER", required = true)
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody DisputeRequest request
    ) {
        enforceRole(userRole, "BUYER");
        Dispute dispute = disputeService.fileDispute(id, userId, request.getReason());
        return toDisputeResponse(dispute);
    }

    @GetMapping("/{id}/dispute")
    @Operation(summary = "Get dispute for a booking")
    public DisputeResponse getDispute(
            @Parameter(description = "Booking id", required = true)
            @PathVariable Long id,
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId
    ) {
        Dispute dispute = disputeService.getDisputeForBooking(id, userId);
        return toDisputeResponse(dispute);
    }

    @PatchMapping("/{id}/confirm-delivery")
    @Operation(summary = "Confirm delivery as buyer")
    public DeliveryConfirmationResponse confirmDelivery(
            @Parameter(description = "Booking id", required = true)
            @PathVariable Long id,
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Current user role, must be BUYER", required = true)
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

    private DisputeResponse toDisputeResponse(Dispute dispute) {
        DisputeResponse response = new DisputeResponse();
        response.setId(dispute.getId());
        response.setBookingId(dispute.getBooking().getId());
        response.setFiledByUserId(dispute.getFiledByUserId());
        response.setReason(dispute.getReason());
        response.setStatus(dispute.getStatus());
        response.setResolutionNote(dispute.getResolutionNote());
        response.setResolvedAt(dispute.getResolvedAt());
        response.setCreatedAt(dispute.getCreatedAt());
        response.setUpdatedAt(dispute.getUpdatedAt());
        return response;
    }

    private void enforceRole(String actualRole, String expectedRole) {
        if (!expectedRole.equalsIgnoreCase(actualRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid user role");
        }
    }
}
