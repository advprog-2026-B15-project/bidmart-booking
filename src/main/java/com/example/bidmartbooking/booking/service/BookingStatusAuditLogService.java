package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.BookingStatusAuditLog;
import com.example.bidmartbooking.booking.repository.BookingStatusAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingStatusAuditLogService {

    private final BookingStatusAuditLogRepository auditLogRepository;

    public BookingStatusAuditLogService(
            BookingStatusAuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public BookingStatusAuditLog recordStatusChange(
            Booking booking,
            BookingStatus fromStatus,
            BookingStatus toStatus,
            String changedByUserId,
            String changedByRole,
            String reason
    ) {
        BookingStatusAuditLog auditLog = new BookingStatusAuditLog();
        auditLog.setBooking(booking);
        auditLog.setFromStatus(fromStatus);
        auditLog.setToStatus(toStatus);
        auditLog.setChangedByUserId(changedByUserId);
        auditLog.setChangedByRole(changedByRole);
        auditLog.setReason(reason);
        return auditLogRepository.save(auditLog);
    }
}
