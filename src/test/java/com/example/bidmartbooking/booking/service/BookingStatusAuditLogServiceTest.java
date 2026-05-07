package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.BookingStatusAuditLog;
import com.example.bidmartbooking.booking.repository.BookingStatusAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingStatusAuditLogServiceTest {

    @Mock
    private BookingStatusAuditLogRepository auditLogRepository;

    @Test
    void shouldRecordStatusChange() {
        BookingStatusAuditLogService service = new BookingStatusAuditLogService(
                auditLogRepository
        );
        Booking booking = new Booking();
        booking.setId(10L);
        when(auditLogRepository.save(org.mockito.Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingStatusAuditLog result = service.recordStatusChange(
                booking,
                BookingStatus.PAID,
                BookingStatus.SHIPPED,
                "seller-10",
                "SELLER",
                "SHIPMENT_STATUS_UPDATED"
        );

        ArgumentCaptor<BookingStatusAuditLog> captor = ArgumentCaptor.forClass(
                BookingStatusAuditLog.class
        );
        verify(auditLogRepository).save(captor.capture());
        BookingStatusAuditLog savedAuditLog = captor.getValue();
        assertEquals(booking, savedAuditLog.getBooking());
        assertEquals(BookingStatus.PAID, savedAuditLog.getFromStatus());
        assertEquals(BookingStatus.SHIPPED, savedAuditLog.getToStatus());
        assertEquals("seller-10", savedAuditLog.getChangedByUserId());
        assertEquals("SELLER", savedAuditLog.getChangedByRole());
        assertEquals("SHIPMENT_STATUS_UPDATED", savedAuditLog.getReason());
        assertEquals(savedAuditLog, result);
    }
}
