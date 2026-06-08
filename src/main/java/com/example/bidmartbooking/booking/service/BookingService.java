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
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final BookingStatusAuditLogService auditLogService;
    private final RealtimeEventService realtimeEventService;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            ShipmentRepository shipmentRepository,
            BookingStatusAuditLogService auditLogService,
            RealtimeEventService realtimeEventService,
            NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.auditLogService = auditLogService;
        this.realtimeEventService = realtimeEventService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(String userId) {
        return bookingRepository.findByBuyerUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getMySellingBookings(String sellerId) {
        return bookingRepository.findBySellerUserIdOrderByCreatedAtDesc(sellerId);
    }

    @Transactional(readOnly = true)
    public Booking getBookingByIdForUser(Long id, String userId) {
        return bookingRepository.findByIdAndBuyerUserId(id, userId)
                .or(() -> bookingRepository.findByIdAndSellerUserId(id, userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));
    }

    @Transactional
    public Booking createBookingFromWinnerEvent(
            String eventId,
            String auctionId,
            String listingId,
            String sellerUserId,
            String winnerUserId,
            Long finalPrice,
            String currency,
            String itemName,
            Integer quantity
    ) {
        Booking existingByEvent = bookingRepository.findBySourceEventId(eventId).orElse(null);
        if (existingByEvent != null) {
            return existingByEvent;
        }

        Booking existingByAuction = bookingRepository.findByAuctionId(auctionId).orElse(null);
        if (existingByAuction != null) {
            return existingByAuction;
        }

        Booking booking = new Booking();
        booking.setSourceEventId(eventId);
        booking.setAuctionId(auctionId);
        booking.setListingId(listingId);
        booking.setSellerUserId(sellerUserId);
        booking.setBuyerUserId(winnerUserId);
        booking.setTotalAmount(finalPrice);
        booking.setCurrency(currency);
        booking.setStatus(BookingStatus.CREATED);

        Booking savedBooking = bookingRepository.save(booking);
        createBookingItem(savedBooking, listingId, itemName, quantity, finalPrice);
        createShipment(savedBooking);
        recordAndPublishStatusChange(
                savedBooking,
                null,
                BookingStatus.CREATED,
                "system",
                "SYSTEM",
                "BOOKING_CREATED_FROM_WINNER_EVENT"
        );

        return savedBooking;
    }

    @Transactional
    public Booking payBookingByAuctionId(String auctionId) {
        Booking booking = bookingRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found for auction " + auctionId
                ));

        if (booking.getStatus() != BookingStatus.CREATED) {
            return booking;
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PAID);
        applyLifecycleTimestamps(booking, BookingStatus.PAID);
        Booking savedBooking = bookingRepository.save(booking);
        recordAndPublishStatusChange(
                savedBooking,
                previousStatus,
                BookingStatus.PAID,
                "system",
                "SYSTEM",
                "BOOKING_PAID_VIA_REST"
        );
        return savedBooking;
    }

    @Transactional
    public Booking payBooking(Long bookingId, String buyerUserId) {
        Booking booking = bookingRepository.findByIdAndBuyerUserId(bookingId, buyerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));

        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking cannot be paid in current status"
            );
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PAID);
        applyLifecycleTimestamps(booking, BookingStatus.PAID);
        Booking savedBooking = bookingRepository.save(booking);
        recordAndPublishStatusChange(
                savedBooking,
                previousStatus,
                BookingStatus.PAID,
                buyerUserId,
                "BUYER",
                "BOOKING_PAID"
        );
        return savedBooking;
    }

    @Transactional
    public Booking transitionBookingStatus(Long bookingId, BookingStatus nextStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));

        BookingStatus currentStatus = booking.getStatus();
        if (currentStatus == null || !currentStatus.canTransitionTo(nextStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid booking status transition"
            );
        }

        booking.setStatus(nextStatus);
        applyLifecycleTimestamps(booking, nextStatus);
        Booking savedBooking = bookingRepository.save(booking);
        recordAndPublishStatusChange(
                savedBooking,
                currentStatus,
                nextStatus,
                "system",
                "SYSTEM",
                "BOOKING_STATUS_TRANSITION"
        );
        return savedBooking;
    }

    @Transactional
    public Shipment updateShipmentForSeller(
            Long bookingId,
            String sellerUserId,
            ShipmentStatus nextStatus,
            String trackingNumber,
            String courierName
    ) {
        Booking booking = bookingRepository.findByIdAndSellerUserId(bookingId, sellerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));

        Shipment shipment = shipmentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found"
                ));

        if (booking.getStatus() != BookingStatus.PAID
                && booking.getStatus() != BookingStatus.SHIPPED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot update shipment before booking is paid"
            );
        }

        ShipmentStatus currentStatus = shipment.getStatus();
        if (currentStatus == null || !currentStatus.canTransitionTo(nextStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid shipment status transition"
            );
        }

        shipment.setStatus(nextStatus);
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            shipment.setTrackingNumber(trackingNumber);
        }
        if (courierName != null && !courierName.isBlank()) {
            shipment.setCourierName(courierName);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BookingStatus previousBookingStatus = booking.getStatus();
        BookingStatus nextBookingStatus = null;

        if (nextStatus == ShipmentStatus.SHIPPED && shipment.getShippedAt() == null) {
            shipment.setShippedAt(now);
            nextBookingStatus = BookingStatus.SHIPPED;
        }
        if (nextStatus == ShipmentStatus.DELIVERED && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(now);
            nextBookingStatus = BookingStatus.DELIVERED;
        }

        if (nextBookingStatus != null) {
            booking.setStatus(nextBookingStatus);
        }

        Booking savedBooking = bookingRepository.save(booking);
        if (nextBookingStatus != null) {
            recordAndPublishStatusChange(
                    savedBooking,
                    previousBookingStatus,
                    nextBookingStatus,
                    sellerUserId,
                    "SELLER",
                    "SHIPMENT_STATUS_UPDATED"
            );
            if (nextBookingStatus == BookingStatus.SHIPPED) {
                notificationService.createShippedNotification(savedBooking.getBuyerUserId(), savedBooking);
            } else if (nextBookingStatus == BookingStatus.DELIVERED) {
                notificationService.createDeliveredNotification(savedBooking.getBuyerUserId(), savedBooking);
            }
        }
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Booking confirmDeliveryForBuyer(Long bookingId, String buyerUserId) {
        Booking booking = bookingRepository.findByIdAndBuyerUserId(bookingId, buyerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));

        if (booking.getStatus() != BookingStatus.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking is not ready for delivery confirmation"
            );
        }

        BookingStatus currentStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        applyLifecycleTimestamps(booking, BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);
        recordAndPublishStatusChange(
                savedBooking,
                currentStatus,
                BookingStatus.COMPLETED,
                buyerUserId,
                "BUYER",
                "BUYER_CONFIRMED_DELIVERY"
        );
        return savedBooking;
    }

    private void recordAndPublishStatusChange(
            Booking booking,
            BookingStatus fromStatus,
            BookingStatus toStatus,
            String changedByUserId,
            String changedByRole,
            String reason
    ) {
        auditLogService.recordStatusChange(
                booking,
                fromStatus,
                toStatus,
                changedByUserId,
                changedByRole,
                reason
        );
        realtimeEventService.publishBookingStatusChange(
                booking,
                fromStatus,
                toStatus,
                changedByUserId,
                changedByRole,
                reason
        );
    }

    private void applyLifecycleTimestamps(Booking booking, BookingStatus nextStatus) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (nextStatus == BookingStatus.PAID && booking.getPaidAt() == null) {
            booking.setPaidAt(now);
        }
        if (nextStatus == BookingStatus.COMPLETED && booking.getCompletedAt() == null) {
            booking.setCompletedAt(now);
        }
    }

    private void createBookingItem(
            Booking booking,
            String listingId,
            String itemName,
            Integer quantity,
            Long finalPrice
    ) {
        int safeQuantity = quantity != null && quantity > 0 ? quantity : 1;
        String safeItemName = itemName != null && !itemName.isBlank()
                ? itemName
                : "Auction Item";

        BookingItem item = new BookingItem();
        item.setBooking(booking);
        item.setListingId(listingId);
        item.setItemName(safeItemName);
        item.setQuantity(safeQuantity);
        item.setUnitPrice(finalPrice);
        item.setSubtotalAmount(finalPrice);
        bookingItemRepository.save(item);
    }

    private void createShipment(Booking booking) {
        Shipment shipment = new Shipment();
        shipment.setBooking(booking);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipmentRepository.save(shipment);
    }
}
