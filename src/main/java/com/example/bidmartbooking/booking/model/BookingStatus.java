package com.example.bidmartbooking.booking.model;

public enum BookingStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    COMPLETED;

    public boolean canTransitionTo(BookingStatus nextStatus) {
        return switch (this) {
            case CREATED -> nextStatus == PAID;
            case PAID -> nextStatus == SHIPPED;
            case SHIPPED -> nextStatus == DELIVERED;
            case DELIVERED -> nextStatus == COMPLETED;
            case COMPLETED -> false;
        };
    }
}
