package com.aashir.notificationservice.service;

import com.aashir.notificationservice.entity.NotificationLog;
import com.aashir.notificationservice.entity.NotificationStatus;
import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.event.NotificationType;
import com.aashir.notificationservice.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private NotificationLogRepository repository;

    @Mock
    private NotificationService notificationService;

    private NotificationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new NotificationProcessor(repository, notificationService);
    }

    private NotificationEvent event(String eventId) {
        return new NotificationEvent(
                eventId,
                NotificationType.USER_REGISTERED,
                Instant.now(),
                "a@b.com",
                "Aashir",
                Map.of());
    }

    @Test
    void newEventIsSentAndMarkedSent() {
        NotificationEvent event = event("evt-1");
        when(repository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        processor.handle(event);

        verify(notificationService).process(event);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository, times(2)).save(captor.capture());
        NotificationLog saved = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertEquals(1, saved.getAttempts());
        assertEquals("evt-1", saved.getEventId());
    }

    @Test
    void alreadySentEventIsSkipped() {
        NotificationEvent event = event("evt-2");
        NotificationLog existing = NotificationLog.pending(event);
        existing.markSent();
        when(repository.findByEventId("evt-2")).thenReturn(Optional.of(existing));

        processor.handle(event);

        verify(notificationService, never()).process(any());
        verify(repository, never()).save(any());
    }

    @Test
    void previouslyFailedEventIsRetriedAndMarkedSent() {
        NotificationEvent event = event("evt-3");
        NotificationLog existing = NotificationLog.pending(event);
        existing.markAttempt();
        existing.markFailed("smtp down");
        when(repository.findByEventId("evt-3")).thenReturn(Optional.of(existing));
        when(repository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        processor.handle(event);

        verify(notificationService).process(event);
        assertEquals(NotificationStatus.SENT, existing.getStatus());
        assertEquals(2, existing.getAttempts());
        assertNull(existing.getErrorMessage());
    }

    @Test
    void sendFailureMarksFailedAndRethrows() {
        NotificationEvent event = event("evt-4");
        when(repository.findByEventId("evt-4")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("smtp down")).when(notificationService).process(event);

        assertThrows(IllegalStateException.class, () -> processor.handle(event));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository, times(2)).save(captor.capture());
        NotificationLog saved = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.FAILED, saved.getStatus());
        assertEquals("smtp down", saved.getErrorMessage());
        assertEquals(1, saved.getAttempts());
    }

    @Test
    void blankEventIdIsRejectedBeforeAnythingElseHappens() {
        assertThrows(IllegalArgumentException.class, () -> processor.handle(event("")));
        assertThrows(IllegalArgumentException.class, () -> processor.handle(event(null)));

        verify(repository, never()).findByEventId(any());
        verify(notificationService, never()).process(any());
    }

    @Test
    void concurrentDuplicateInsertIsIgnored() {
        NotificationEvent event = event("evt-5");
        when(repository.findByEventId("evt-5")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationLog.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        processor.handle(event);

        verify(notificationService, never()).process(any());
    }
}