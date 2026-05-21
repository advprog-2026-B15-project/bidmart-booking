package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Dispute;
import com.example.bidmartbooking.booking.model.DisputeStatus;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.DisputeRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusAuditLogService auditLogService;
    private final RealtimeEventService realtimeEventService;

    public DisputeService(
            DisputeRepository disputeRepository,
            BookingRepository bookingRepository,
            BookingStatusAuditLogService auditLogService,
            RealtimeEventService realtimeEventService
    ) {
        this.disputeRepository = disputeRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogService = auditLogService;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public Dispute fileDispute(Long bookingId, String buyerUserId, String reason) {
        Booking booking = bookingRepository.findByIdAndBuyerUserId(bookingId, buyerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));

        if (disputeRepository.existsByBookingId(bookingId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Dispute already exists for this booking"
            );
        }

        if (booking.getStatus() != BookingStatus.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Dispute can only be filed after delivery"
            );
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.DISPUTED);
        booking.setDisputedAt(OffsetDateTime.now(ZoneOffset.UTC));
        bookingRepository.save(booking);

        auditLogService.recordStatusChange(
                booking,
                previousStatus,
                BookingStatus.DISPUTED,
                buyerUserId,
                "BUYER",
                "BUYER_FILED_DISPUTE"
        );
        realtimeEventService.publishBookingStatusChange(
                booking,
                previousStatus,
                BookingStatus.DISPUTED,
                buyerUserId,
                "BUYER",
                "BUYER_FILED_DISPUTE"
        );

        Dispute dispute = new Dispute();
        dispute.setBooking(booking);
        dispute.setFiledByUserId(buyerUserId);
        dispute.setReason(reason);
        dispute.setStatus(DisputeStatus.OPEN);
        return disputeRepository.save(dispute);
    }

    @Transactional(readOnly = true)
    public Dispute getDisputeForBooking(Long bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));

        boolean isBuyer = booking.getBuyerUserId().equals(userId);
        boolean isSeller = booking.getSellerUserId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return disputeRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No dispute found for this booking"
                ));
    }
}
