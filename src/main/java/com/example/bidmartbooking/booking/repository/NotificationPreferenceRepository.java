package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.NotificationPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserId(String userId);
}
