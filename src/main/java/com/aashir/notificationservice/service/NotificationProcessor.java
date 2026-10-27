package com.aashir.notificationservice.service;

import com.aashir.notificationservice.entity.NotificationLog;
import com.aashir.notificationservice.entity.NotificationStatus;
import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);

    private final NotificationLogRepository repository;
    private final NotificationService notificationService;

    public NotificationProcessor(NotificationLogRepository repository,
                                 NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void handle(NotificationEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }

        NotificationLog entry = repository.findByEventId(event.eventId()).orElse(null);

        if (entry != null && entry.getStatus() == NotificationStatus.SENT) {
            log.info("Duplicate event skipped: id={}", event.eventId());
            return;
        }

        if (entry == null) {
            try {
                entry = repository.save(NotificationLog.pending(event));
            } catch (DataIntegrityViolationException e) {
                log.info("Event already being processed: id={}", event.eventId());
                return;
            }
        }

        entry.markAttempt();

        try {
            notificationService.process(event);
            entry.markSent();
            repository.save(entry);
            log.info("Notification sent: id={}, type={}", event.eventId(), event.type());
        } catch (RuntimeException e) {
            entry.markFailed(e.getMessage());
            repository.save(entry);
            log.error("Notification failed: id={}, type={}", event.eventId(), event.type(), e);
            throw e;
        }
    }
}