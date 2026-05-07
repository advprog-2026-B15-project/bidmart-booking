package com.example.bidmartbooking.booking.integration;

import com.example.bidmartbooking.booking.repository.NotificationPreferenceRepository;
import com.example.bidmartbooking.booking.repository.NotificationRepository;
import com.example.bidmartbooking.booking.service.NotificationService;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationStreamIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
    }

    @Test
    void shouldDeliverNotificationThroughSseStream() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/notifications/stream"))
                .header("X-User-Id", "stream-user")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<InputStream> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        assertEquals(200, response.statusCode());

        try (InputStream body = response.body();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        body,
                        StandardCharsets.UTF_8
                ))) {
            assertTrue(readUntil(reader, "event:connected"));

            notificationService.createBalanceConvertedNotification(
                    "stream-user",
                    "auc-stream",
                    120000L
            );

            assertTrue(readUntil(reader, "event:notification"));
            assertTrue(readUntil(reader, "PAYMENT_CONFIRMED"));
        }
    }

    private boolean readUntil(BufferedReader reader, String expectedText) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && line.contains(expectedText)) {
                    return true;
                }
                continue;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
