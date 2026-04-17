package com.example.bidmartbooking.booking.model;

public enum ShipmentStatus {
    PENDING,
    SHIPPED,
    DELIVERED;

    public boolean canTransitionTo(ShipmentStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == SHIPPED;
            case SHIPPED -> nextStatus == DELIVERED;
            case DELIVERED -> false;
        };
    }
}
