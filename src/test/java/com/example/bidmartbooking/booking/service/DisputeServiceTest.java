package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Dispute;
import com.example.bidmartbooking.booking.model.DisputeStatus;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.DisputeRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingStatusAuditLogService auditLogService;

    @Mock
    private RealtimeEventService realtimeEventService;

    private DisputeService disputeService;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(
                disputeRepository,
                bookingRepository,
                auditLogService,
                realtimeEventService
        );
    }

    @Test
    void shouldFileDisputeSuccessfully() {
        Booking booking = deliveredBooking(10L, "buyer-10", "seller-10");

        when(bookingRepository.findByIdAndBuyerUserId(10L, "buyer-10"))
                .thenReturn(Optional.of(booking));
        when(disputeRepository.existsByBookingId(10L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute savedDispute = new Dispute();
        savedDispute.setId(1L);
        savedDispute.setBooking(booking);
        savedDispute.setFiledByUserId("buyer-10");
        savedDispute.setReason("Barang tidak sesuai deskripsi");
        savedDispute.setStatus(DisputeStatus.OPEN);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        Dispute result = disputeService.fileDispute(10L, "buyer-10", "Barang tidak sesuai deskripsi");

        assertEquals(DisputeStatus.OPEN, result.getStatus());
        assertEquals("buyer-10", result.getFiledByUserId());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(BookingStatus.DISPUTED, bookingCaptor.getValue().getStatus());
        assertNotNull(bookingCaptor.getValue().getDisputedAt());
    }

    @Test
    void shouldPublishAuditAndRealtimeWhenDisputeFiled() {
        Booking booking = deliveredBooking(11L, "buyer-11", "seller-11");

        when(bookingRepository.findByIdAndBuyerUserId(11L, "buyer-11"))
                .thenReturn(Optional.of(booking));
        when(disputeRepository.existsByBookingId(11L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        disputeService.fileDispute(11L, "buyer-11", "Produk cacat saat diterima");

        verify(auditLogService).recordStatusChange(
                any(Booking.class),
                org.mockito.ArgumentMatchers.eq(BookingStatus.DELIVERED),
                org.mockito.ArgumentMatchers.eq(BookingStatus.DISPUTED),
                org.mockito.ArgumentMatchers.eq("buyer-11"),
                org.mockito.ArgumentMatchers.eq("BUYER"),
                org.mockito.ArgumentMatchers.eq("BUYER_FILED_DISPUTE")
        );
        verify(realtimeEventService).publishBookingStatusChange(
                any(Booking.class),
                org.mockito.ArgumentMatchers.eq(BookingStatus.DELIVERED),
                org.mockito.ArgumentMatchers.eq(BookingStatus.DISPUTED),
                org.mockito.ArgumentMatchers.eq("buyer-11"),
                org.mockito.ArgumentMatchers.eq("BUYER"),
                org.mockito.ArgumentMatchers.eq("BUYER_FILED_DISPUTE")
        );
    }

    @Test
    void shouldThrowNotFoundWhenBuyerDoesNotOwnBooking() {
        when(bookingRepository.findByIdAndBuyerUserId(20L, "buyer-99"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> disputeService.fileDispute(20L, "buyer-99", "alasan valid sekali")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void shouldThrowConflictWhenDisputeAlreadyExists() {
        Booking booking = deliveredBooking(40L, "buyer-40", "seller-40");

        when(bookingRepository.findByIdAndBuyerUserId(40L, "buyer-40"))
                .thenReturn(Optional.of(booking));
        when(disputeRepository.existsByBookingId(40L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> disputeService.fileDispute(40L, "buyer-40", "alasan valid sekali")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenBookingNotDelivered() {
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setBuyerUserId("buyer-30");
        booking.setStatus(BookingStatus.SHIPPED);

        when(bookingRepository.findByIdAndBuyerUserId(30L, "buyer-30"))
                .thenReturn(Optional.of(booking));
        when(disputeRepository.existsByBookingId(30L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> disputeService.fileDispute(30L, "buyer-30", "alasan valid sekali")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldGetDisputeForBuyer() {
        Booking booking = deliveredBooking(50L, "buyer-50", "seller-50");
        Dispute dispute = disputeFor(booking, "buyer-50");

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(disputeRepository.findByBookingId(50L)).thenReturn(Optional.of(dispute));

        Dispute result = disputeService.getDisputeForBooking(50L, "buyer-50");

        assertEquals("buyer-50", result.getFiledByUserId());
    }

    @Test
    void shouldGetDisputeForSeller() {
        Booking booking = deliveredBooking(51L, "buyer-51", "seller-51");
        Dispute dispute = disputeFor(booking, "buyer-51");

        when(bookingRepository.findById(51L)).thenReturn(Optional.of(booking));
        when(disputeRepository.findByBookingId(51L)).thenReturn(Optional.of(dispute));

        Dispute result = disputeService.getDisputeForBooking(51L, "seller-51");

        assertNotNull(result);
    }

    @Test
    void shouldThrowForbiddenWhenUnrelatedUserAccessesDispute() {
        Booking booking = deliveredBooking(52L, "buyer-52", "seller-52");

        when(bookingRepository.findById(52L)).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> disputeService.getDisputeForBooking(52L, "random-user")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void shouldThrowNotFoundWhenBookingMissingForGetDispute() {
        when(bookingRepository.findById(60L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> disputeService.getDisputeForBooking(60L, "buyer-60")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void shouldThrowNotFoundWhenDisputeDoesNotExist() {
        Booking booking = deliveredBooking(61L, "buyer-61", "seller-61");

        when(bookingRepository.findById(61L)).thenReturn(Optional.of(booking));
        when(disputeRepository.findByBookingId(61L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> disputeService.getDisputeForBooking(61L, "buyer-61")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private Booking deliveredBooking(Long id, String buyerUserId, String sellerUserId) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setBuyerUserId(buyerUserId);
        booking.setSellerUserId(sellerUserId);
        booking.setStatus(BookingStatus.DELIVERED);
        return booking;
    }

    private Dispute disputeFor(Booking booking, String filedByUserId) {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setBooking(booking);
        dispute.setFiledByUserId(filedByUserId);
        dispute.setReason("Barang tidak sesuai");
        dispute.setStatus(DisputeStatus.OPEN);
        return dispute;
    }
}
