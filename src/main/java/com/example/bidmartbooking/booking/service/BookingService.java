package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(String userId) {
        return bookingRepository.findByBuyerUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Booking getBookingByIdForUser(Long id, String userId) {
        return bookingRepository.findByIdAndBuyerUserId(id, userId)
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
            String currency
    ) {
        if (bookingRepository.existsBySourceEventId(eventId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event already processed");
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

        return bookingRepository.save(booking);
    }
}
