package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingItem;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Shipment;
import com.example.bidmartbooking.booking.model.ShipmentStatus;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
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

    public BookingService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            ShipmentRepository shipmentRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.shipmentRepository = shipmentRepository;
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
            String currency,
            String itemName,
            Integer quantity
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

        Booking savedBooking = bookingRepository.save(booking);
        createBookingItem(savedBooking, listingId, itemName, quantity, finalPrice);
        createShipment(savedBooking);

        return savedBooking;
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
