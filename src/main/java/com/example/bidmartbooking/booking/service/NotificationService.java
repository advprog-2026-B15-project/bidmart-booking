package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.dto.RealtimeAuctionUpdateResponse;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationPreference;
import com.example.bidmartbooking.booking.model.NotificationType;
import com.example.bidmartbooking.booking.repository.NotificationPreferenceRepository;
import com.example.bidmartbooking.booking.repository.NotificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final RealtimeEventService realtimeEventService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            RealtimeEventService realtimeEventService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreference getMyNotificationPreference(String userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUserId(userId);
                    preference.setEmailEnabled(false);
                    preference.setInAppEnabled(true);
                    return preference;
                });
    }

    @Transactional
    public NotificationPreference upsertNotificationPreference(
            String userId,
            Boolean emailEnabled,
            Boolean inAppEnabled
    ) {
        NotificationPreference preference = notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setUserId(userId);
                    return created;
                });

        if (emailEnabled != null) {
            preference.setEmailEnabled(emailEnabled);
        }
        if (inAppEnabled != null) {
            preference.setInAppEnabled(inAppEnabled);
        }

        return notificationPreferenceRepository.save(preference);
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

        addIfInAppEnabled(
                notifications,
                winnerUserId,
                NotificationType.WIN,
                "You won the auction",
                "You won auction " + auctionId + " with final price IDR " + finalPrice,
                auctionId
        );

        for (String loserUserId : loserUserIds) {
            addIfInAppEnabled(
                    notifications,
                    loserUserId,
                    NotificationType.LOSE,
                    "You lost the auction",
                    "You were outbid in auction " + auctionId,
                    auctionId
            );
        }

        saveNotifications(notifications);
        publishBidPlacedAuctionUpdates(
                sellerUserId,
                bidderUserId,
                previousHighestBidderUserId,
                auctionId,
                bidAmount,
                safeItemName
        );
    }

    @Transactional
    public void createBidPlacedNotifications(
            String sellerUserId,
            String bidderUserId,
            String previousHighestBidderUserId,
            String auctionId,
            Long bidAmount,
            String itemName
    ) {
        List<Notification> notifications = new ArrayList<>();

        String safeItemName = itemName != null && !itemName.isBlank()
                ? itemName
                : "Auction Item";

        addIfInAppEnabled(
                notifications,
                sellerUserId,
                NotificationType.NEW_BID,
                "New bid placed",
                "A new bid of IDR " + bidAmount + " was placed for " + safeItemName,
                auctionId
        );

        if (previousHighestBidderUserId != null
                && !previousHighestBidderUserId.isBlank()
                && !previousHighestBidderUserId.equals(bidderUserId)) {
            addIfInAppEnabled(
                    notifications,
                    previousHighestBidderUserId,
                    NotificationType.OUTBID,
                    "You have been outbid",
                    "Another bidder placed IDR " + bidAmount + " for " + safeItemName,
                    auctionId
            );
        }

        saveNotifications(notifications);
    }

    @Transactional
    public void createBalanceConvertedNotification(
            String userId,
            String auctionId,
            Long amount
    ) {
        saveNotificationIfInAppEnabled(
                userId,
                NotificationType.PAYMENT_CONFIRMED,
                "Payment confirmed",
                "Your payment of IDR " + amount + " has been confirmed",
                auctionId
        );
    }

    @Transactional
    public void createBalanceReleasedNotification(
            String userId,
            String auctionId,
            Long amount
    ) {
        saveNotificationIfInAppEnabled(
                userId,
                NotificationType.BALANCE_RELEASED,
                "Balance released",
                "Your balance release of IDR " + amount + " has been processed",
                auctionId
        );
    }

    private void addIfInAppEnabled(
            List<Notification> notifications,
            String userId,
            NotificationType type,
            String title,
            String message,
            String auctionId
    ) {
        if (!isInAppEnabled(userId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedAuctionId(auctionId);
        notifications.add(notification);
    }

    private void saveNotificationIfInAppEnabled(
            String userId,
            NotificationType type,
            String title,
            String message,
            String auctionId
    ) {
        if (!isInAppEnabled(userId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedAuctionId(auctionId);
        Notification savedNotification = notificationRepository.save(notification);
        realtimeEventService.publishNotification(savedNotification);
    }

    private boolean isInAppEnabled(String userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(NotificationPreference::getInAppEnabled)
                .orElse(true);
    }

    private void saveNotifications(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        savedNotifications.forEach(realtimeEventService::publishNotification);
    }

    private void publishBidPlacedAuctionUpdates(
            String sellerUserId,
            String bidderUserId,
            String previousHighestBidderUserId,
            String auctionId,
            Long bidAmount,
            String itemName
    ) {
        RealtimeAuctionUpdateResponse update = new RealtimeAuctionUpdateResponse();
        update.setAuctionId(auctionId);
        update.setItemName(itemName);
        update.setCurrentPrice(bidAmount);
        update.setEventType("BidPlaced");

        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(sellerUserId);
        recipients.add(bidderUserId);
        if (previousHighestBidderUserId != null
                && !previousHighestBidderUserId.isBlank()) {
            recipients.add(previousHighestBidderUserId);
        }

        recipients.forEach(userId -> realtimeEventService.publishAuctionUpdate(
                userId,
                update
        ));
    }
}
