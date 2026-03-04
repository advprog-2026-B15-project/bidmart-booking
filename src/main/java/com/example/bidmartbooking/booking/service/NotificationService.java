package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationType;
import com.example.bidmartbooking.booking.repository.NotificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Notification markNotificationAsRead(Long id, String userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found"
                ));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
        }

        return notificationRepository.save(notification);
    }

    @Transactional
    public void createWinLoseNotifications(
            String winnerUserId,
            List<String> loserUserIds,
            String auctionId,
            Long finalPrice
    ) {
        List<Notification> notifications = new ArrayList<>();

        Notification winnerNotif = new Notification();
        winnerNotif.setUserId(winnerUserId);
        winnerNotif.setType(NotificationType.WIN);
        winnerNotif.setTitle("You won the auction");
        winnerNotif.setMessage("You won auction " + auctionId + " with final price IDR " + finalPrice);
        winnerNotif.setRelatedAuctionId(auctionId);
        notifications.add(winnerNotif);

        for (String loserUserId : loserUserIds) {
            Notification loserNotif = new Notification();
            loserNotif.setUserId(loserUserId);
            loserNotif.setType(NotificationType.LOSE);
            loserNotif.setTitle("You lost the auction");
            loserNotif.setMessage("You were outbid in auction " + auctionId);
            loserNotif.setRelatedAuctionId(auctionId);
            notifications.add(loserNotif);
        }

        notificationRepository.saveAll(notifications);
    }
}
