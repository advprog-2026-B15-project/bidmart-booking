package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationPreference;
import com.example.bidmartbooking.booking.model.NotificationType;
import com.example.bidmartbooking.booking.repository.NotificationPreferenceRepository;
import com.example.bidmartbooking.booking.repository.NotificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private RealtimeEventService realtimeEventService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationPreferenceRepository,
                realtimeEventService
        );
        lenient().when(notificationPreferenceRepository.findByUserId(anyString()))
                .thenReturn(Optional.empty());
        lenient().when(notificationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnMyNotifications() {
        Notification notification = new Notification();
        notification.setId(1L);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("usr-1"))
                .thenReturn(List.of(notification));

        List<Notification> result = notificationService.getMyNotifications("usr-1");

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
    }

    @Test
    void shouldThrowWhenMarkReadNotificationNotFound() {
        when(notificationRepository.findByIdAndUserId(1L, "usr-1"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> notificationService.markNotificationAsRead(1L, "usr-1")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldMarkUnreadNotificationAsRead() {
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setIsRead(false);

        when(notificationRepository.findByIdAndUserId(10L, "usr-1"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markNotificationAsRead(10L, "usr-1");

        assertEquals(true, result.getIsRead());
        assertNotNull(result.getReadAt());
    }

    @Test
    void shouldKeepAlreadyReadNotification() {
        OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        Notification notification = new Notification();
        notification.setId(11L);
        notification.setIsRead(true);
        notification.setReadAt(readAt);

        when(notificationRepository.findByIdAndUserId(11L, "usr-1"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markNotificationAsRead(11L, "usr-1");

        assertEquals(true, result.getIsRead());
        assertEquals(readAt, result.getReadAt());
    }

    @Test
    void shouldCreateWinAndLoseNotifications() {
        Booking booking = new Booking();
        booking.setId(10L);

        notificationService.createWinLoseNotifications(
                "winner-1",
                List.of("loser-1", "loser-2"),
                "auc-1",
                900000L,
                booking
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(3, notifications.size());

        Notification winner = notifications.get(0);
        assertEquals("winner-1", winner.getUserId());
        assertEquals(NotificationType.WIN, winner.getType());
        assertEquals(booking, winner.getRelatedBooking());

        Notification loser = notifications.get(1);
        assertEquals(NotificationType.LOSE, loser.getType());
        assertEquals("loser-1", loser.getUserId());
        verify(realtimeEventService).publishNotification(winner);
    }

    @Test
    void shouldSkipUsersWithInAppDisabledForWinLoseNotifications() {
        NotificationPreference disabledPreference = new NotificationPreference();
        disabledPreference.setInAppEnabled(false);

        NotificationPreference enabledPreference = new NotificationPreference();
        enabledPreference.setInAppEnabled(true);

        when(notificationPreferenceRepository.findByUserId("winner-off"))
                .thenReturn(Optional.of(disabledPreference));
        when(notificationPreferenceRepository.findByUserId("loser-off"))
                .thenReturn(Optional.of(disabledPreference));
        when(notificationPreferenceRepository.findByUserId("loser-on"))
                .thenReturn(Optional.of(enabledPreference));

        notificationService.createWinLoseNotifications(
                "winner-off",
                List.of("loser-off", "loser-on"),
                "auc-pref-1",
                910000L,
                null
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(1, notifications.size());
        assertEquals("loser-on", notifications.getFirst().getUserId());
        assertEquals(NotificationType.LOSE, notifications.getFirst().getType());
    }

    @Test
    void shouldNotSaveWinLoseNotificationsWhenAllRecipientsDisableInApp() {
        NotificationPreference disabledPreference = new NotificationPreference();
        disabledPreference.setInAppEnabled(false);

        when(notificationPreferenceRepository.findByUserId("winner-off"))
                .thenReturn(Optional.of(disabledPreference));
        when(notificationPreferenceRepository.findByUserId("loser-off"))
                .thenReturn(Optional.of(disabledPreference));

        notificationService.createWinLoseNotifications(
                "winner-off",
                List.of("loser-off"),
                "auc-pref-2",
                920000L,
                null
        );

        verify(notificationRepository, never()).saveAll(any());
        verify(realtimeEventService, never()).publishNotification(any());
    }

    @Test
    void shouldReturnStoredNotificationPreference() {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId("usr-pref");
        preference.setEmailEnabled(true);
        preference.setInAppEnabled(false);

        when(notificationPreferenceRepository.findByUserId("usr-pref"))
                .thenReturn(Optional.of(preference));

        NotificationPreference result =
                notificationService.getMyNotificationPreference("usr-pref");

        assertEquals("usr-pref", result.getUserId());
        assertEquals(true, result.getEmailEnabled());
        assertEquals(false, result.getInAppEnabled());
    }

    @Test
    void shouldReturnDefaultPreferenceWhenMissing() {
        when(notificationPreferenceRepository.findByUserId("usr-new"))
                .thenReturn(Optional.empty());

        NotificationPreference result =
                notificationService.getMyNotificationPreference("usr-new");

        assertEquals("usr-new", result.getUserId());
        result.prePersist();
        assertEquals(false, result.getEmailEnabled());
        assertEquals(true, result.getInAppEnabled());
    }

    @Test
    void shouldCreateNotificationPreferenceWhenMissing() {
        when(notificationPreferenceRepository.findByUserId("usr-create"))
                .thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = notificationService.upsertNotificationPreference(
                "usr-create",
                true,
                false
        );

        assertEquals("usr-create", result.getUserId());
        assertEquals(true, result.getEmailEnabled());
        assertEquals(false, result.getInAppEnabled());
    }

    @Test
    void shouldUpdateExistingNotificationPreference() {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId("usr-update");
        preference.setEmailEnabled(false);
        preference.setInAppEnabled(true);

        when(notificationPreferenceRepository.findByUserId("usr-update"))
                .thenReturn(Optional.of(preference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = notificationService.upsertNotificationPreference(
                "usr-update",
                true,
                null
        );

        assertEquals(true, result.getEmailEnabled());
        assertEquals(true, result.getInAppEnabled());
    }

    @Test
    void shouldKeepExistingEmailPreferenceWhenEmailFlagIsNull() {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId("usr-keep-email");
        preference.setEmailEnabled(true);
        preference.setInAppEnabled(false);

        when(notificationPreferenceRepository.findByUserId("usr-keep-email"))
                .thenReturn(Optional.of(preference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = notificationService.upsertNotificationPreference(
                "usr-keep-email",
                null,
                true
        );

        assertEquals(true, result.getEmailEnabled());
        assertEquals(true, result.getInAppEnabled());
    }

    @Test
    void shouldCreateBidPlacedNotificationsForSellerAndOutbidUser() {
        notificationService.createBidPlacedNotifications(
                "seller-1",
                "bidder-2",
                "bidder-1",
                "auc-bid",
                150000L,
                "Keyboard"
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(2, notifications.size());
        assertEquals(NotificationType.NEW_BID, notifications.get(0).getType());
        assertEquals("seller-1", notifications.get(0).getUserId());
        assertEquals(NotificationType.OUTBID, notifications.get(1).getType());
        assertEquals("bidder-1", notifications.get(1).getUserId());
        verify(realtimeEventService).publishAuctionUpdate(eq("seller-1"), any());
        verify(realtimeEventService).publishAuctionUpdate(eq("bidder-2"), any());
        verify(realtimeEventService).publishAuctionUpdate(eq("bidder-1"), any());
    }

    @Test
    void shouldCreateOnlySellerNotificationWhenPreviousHighestBidderMissing() {
        notificationService.createBidPlacedNotifications(
                "seller-2",
                "bidder-2",
                null,
                "auc-bid-2",
                250000L,
                null
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(1, notifications.size());
        assertEquals(NotificationType.NEW_BID, notifications.getFirst().getType());
        assertEquals("seller-2", notifications.getFirst().getUserId());
        assertEquals("A new bid of IDR 250000 was placed for Auction Item",
                notifications.getFirst().getMessage());
    }

    @Test
    void shouldSkipOutbidNotificationWhenPreviousHighestBidderEqualsBidder() {
        notificationService.createBidPlacedNotifications(
                "seller-3",
                "bidder-3",
                "bidder-3",
                "auc-bid-3",
                350000L,
                "Mouse"
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(1, notifications.size());
        assertEquals(NotificationType.NEW_BID, notifications.getFirst().getType());
    }

    @Test
    void shouldSkipOutbidNotificationWhenPreviousHighestBidderBlank() {
        notificationService.createBidPlacedNotifications(
                "seller-4",
                "bidder-4",
                " ",
                "auc-bid-4",
                450000L,
                " "
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(1, notifications.size());
        assertEquals("A new bid of IDR 450000 was placed for Auction Item",
                notifications.getFirst().getMessage());
    }

    @Test
    void shouldSkipDisabledSellerAndKeepEnabledOutbidUser() {
        NotificationPreference disabledPreference = new NotificationPreference();
        disabledPreference.setInAppEnabled(false);

        NotificationPreference enabledPreference = new NotificationPreference();
        enabledPreference.setInAppEnabled(true);

        when(notificationPreferenceRepository.findByUserId("seller-off"))
                .thenReturn(Optional.of(disabledPreference));
        when(notificationPreferenceRepository.findByUserId("outbid-on"))
                .thenReturn(Optional.of(enabledPreference));

        notificationService.createBidPlacedNotifications(
                "seller-off",
                "bidder-7",
                "outbid-on",
                "auc-pref-bid",
                330000L,
                "Headset"
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(1, notifications.size());
        assertEquals("outbid-on", notifications.getFirst().getUserId());
        assertEquals(NotificationType.OUTBID, notifications.getFirst().getType());
    }

    @Test
    void shouldCreateBalanceConvertedNotification() {
        notificationService.createBalanceConvertedNotification(
                "buyer-1",
                "auc-pay",
                500000L
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals("buyer-1", saved.getUserId());
        assertEquals(NotificationType.PAYMENT_CONFIRMED, saved.getType());
        assertEquals("auc-pay", saved.getRelatedAuctionId());
        verify(realtimeEventService).publishNotification(saved);
    }

    @Test
    void shouldSkipBalanceConvertedNotificationWhenInAppDisabled() {
        NotificationPreference disabledPreference = new NotificationPreference();
        disabledPreference.setInAppEnabled(false);

        when(notificationPreferenceRepository.findByUserId("buyer-off"))
                .thenReturn(Optional.of(disabledPreference));

        notificationService.createBalanceConvertedNotification(
                "buyer-off",
                "auc-pay-off",
                500000L
        );

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(realtimeEventService, never()).publishNotification(any());
    }

    @Test
    void shouldCreateBalanceReleasedNotification() {
        notificationService.createBalanceReleasedNotification(
                "seller-1",
                "auc-release",
                500000L
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals("seller-1", saved.getUserId());
        assertEquals(NotificationType.BALANCE_RELEASED, saved.getType());
        assertEquals("auc-release", saved.getRelatedAuctionId());
        verify(realtimeEventService).publishNotification(saved);
    }

    @Test
    void shouldSkipBalanceReleasedNotificationWhenInAppDisabled() {
        NotificationPreference disabledPreference = new NotificationPreference();
        disabledPreference.setInAppEnabled(false);

        when(notificationPreferenceRepository.findByUserId("seller-off"))
                .thenReturn(Optional.of(disabledPreference));

        notificationService.createBalanceReleasedNotification(
                "seller-off",
                "auc-release-off",
                500000L
        );

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(realtimeEventService, never()).publishNotification(any());
    }
}
