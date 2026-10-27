package com.aashir.notificationservice.entity;

import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.event.NotificationType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_log",uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id",nullable = false,length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(length = 1000)
    private String errorMessage;

    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected NotificationLog() {
    }

    public static NotificationLog pending(NotificationEvent event) {
        NotificationLog entry = new NotificationLog();
        entry.eventId = event.eventId();
        entry.type = event.type();
        entry.recipientEmail = event.recipientEmail();
        entry.status = NotificationStatus.PENDING;
        entry.createdAt = Instant.now();
        return entry;
    }

    public void markAttempt() {
        this.attempts++;
        this.updatedAt = Instant.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = message == null ? null : message.substring(0, Math.min(message.length(), 1000));
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }


}
