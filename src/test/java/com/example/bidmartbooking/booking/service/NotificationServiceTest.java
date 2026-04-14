package com.example.bidmartbooking.booking.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationPreferenceRepository
        );
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
        notificationService.createWinLoseNotifications(
                "winner-1",
                List.of("loser-1", "loser-2"),
                "auc-1",
                900000L
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertEquals(3, notifications.size());

        Notification winner = notifications.get(0);
        assertEquals("winner-1", winner.getUserId());
        assertEquals(NotificationType.WIN, winner.getType());

        Notification loser = notifications.get(1);
        assertEquals(NotificationType.LOSE, loser.getType());
        assertEquals("loser-1", loser.getUserId());
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
}
