package com.example.bidmartbooking.booking.bootstrap;

import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.example.bidmartbooking.booking.event.WinnerDeterminedPayload;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDummyDataSeederTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingEventConsumer bookingEventConsumer;

    private BookingDummyDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new BookingDummyDataSeeder(bookingRepository, bookingEventConsumer);
    }

    @Test
    void shouldSkipSeedWhenDataAlreadyExists() throws Exception {
        when(bookingRepository.count()).thenReturn(1L);

        seeder.run(new DefaultApplicationArguments(new String[]{}));

        verify(bookingEventConsumer, never()).handleWinnerDetermined(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSeedTwoWinnerEventsWhenDatabaseEmpty() throws Exception {
        when(bookingRepository.count()).thenReturn(0L);

        seeder.run(new DefaultApplicationArguments(new String[]{}));

        ArgumentCaptor<EventEnvelope<WinnerDeterminedPayload>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(bookingEventConsumer, times(2)).handleWinnerDetermined(captor.capture());

        EventEnvelope<WinnerDeterminedPayload> first = captor.getAllValues().getFirst();
        assertEquals("evt-seed-001", first.getEventId());
        assertEquals("WinnerDetermined", first.getEventType());
        assertEquals("auc-seed-001", first.getPayload().getAuctionId());
        assertEquals("IDR", first.getPayload().getCurrency());

        EventEnvelope<WinnerDeterminedPayload> second = captor.getAllValues().get(1);
        assertEquals("evt-seed-002", second.getEventId());
        assertEquals("auc-seed-002", second.getPayload().getAuctionId());
    }
}
