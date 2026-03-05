package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationType;
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

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
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
}
