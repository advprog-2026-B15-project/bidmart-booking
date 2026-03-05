package com.example.bidmartbooking.booking.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityLifecycleTest {

    @Test
    void bookingPrePersistShouldSetDefaults() {
        Booking booking = new Booking();

        booking.prePersist();

        assertEquals(BookingStatus.CREATED, booking.getStatus());
        assertEquals("IDR", booking.getCurrency());
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getUpdatedAt());
    }

    @Test
    void bookingPrePersistShouldKeepExistingValues() {
        Booking booking = new Booking();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5);
        booking.setStatus(BookingStatus.SHIPPED);
        booking.setCurrency("USD");
        booking.setCreatedAt(createdAt);
        booking.setUpdatedAt(updatedAt);

        booking.prePersist();

        assertEquals(BookingStatus.SHIPPED, booking.getStatus());
        assertEquals("USD", booking.getCurrency());
        assertEquals(createdAt, booking.getCreatedAt());
        assertEquals(updatedAt, booking.getUpdatedAt());
    }

    @Test
    void bookingPreUpdateShouldRefreshUpdatedAt() {
        Booking booking = new Booking();
        OffsetDateTime old = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        booking.setUpdatedAt(old);

        booking.preUpdate();

        assertTrue(booking.getUpdatedAt().isAfter(old));
    }

    @Test
    void bookingItemPrePersistShouldSetCreatedAtWhenNull() {
        BookingItem bookingItem = new BookingItem();

        bookingItem.prePersist();

        assertNotNull(bookingItem.getCreatedAt());
    }

    @Test
    void bookingItemPrePersistShouldKeepCreatedAtWhenAlreadySet() {
        BookingItem bookingItem = new BookingItem();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2);
        bookingItem.setCreatedAt(createdAt);

        bookingItem.prePersist();

        assertEquals(createdAt, bookingItem.getCreatedAt());
    }

    @Test
    void shipmentPrePersistShouldSetDefaults() {
        Shipment shipment = new Shipment();

        shipment.prePersist();

        assertEquals(ShipmentStatus.PENDING, shipment.getStatus());
        assertNotNull(shipment.getCreatedAt());
        assertNotNull(shipment.getUpdatedAt());
    }

    @Test
    void shipmentPrePersistShouldKeepExistingValues() {
        Shipment shipment = new Shipment();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);

        shipment.prePersist();

        assertEquals(ShipmentStatus.SHIPPED, shipment.getStatus());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertEquals(updatedAt, shipment.getUpdatedAt());
    }

    @Test
    void shipmentPreUpdateShouldRefreshUpdatedAt() {
        Shipment shipment = new Shipment();
        OffsetDateTime old = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        shipment.setUpdatedAt(old);

        shipment.preUpdate();

        assertTrue(shipment.getUpdatedAt().isAfter(old));
    }

    @Test
    void notificationPrePersistShouldSetDefaults() {
        Notification notification = new Notification();

        notification.prePersist();

        assertEquals(false, notification.getIsRead());
        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.getUpdatedAt());
    }

    @Test
    void notificationPrePersistShouldKeepExistingValues() {
        Notification notification = new Notification();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(6);
        OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(20);
        notification.setIsRead(true);
        notification.setCreatedAt(createdAt);
        notification.setUpdatedAt(updatedAt);

        notification.prePersist();

        assertEquals(true, notification.getIsRead());
        assertEquals(createdAt, notification.getCreatedAt());
        assertEquals(updatedAt, notification.getUpdatedAt());
    }

    @Test
    void notificationPreUpdateShouldRefreshUpdatedAt() {
        Notification notification = new Notification();
        OffsetDateTime old = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2);
        notification.setUpdatedAt(old);

        notification.preUpdate();

        assertTrue(notification.getUpdatedAt().isAfter(old));
    }

    @Test
    void enumsShouldContainExpectedValues() {
        assertNotNull(BookingStatus.valueOf("CREATED"));
        assertNotNull(ShipmentStatus.valueOf("PENDING"));
        assertNotNull(NotificationType.valueOf("WIN"));
    }
}
