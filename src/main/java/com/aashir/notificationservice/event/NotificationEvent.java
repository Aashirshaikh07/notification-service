package com.aashir.notificationservice.event;

import java.time.Instant;
import java.util.Map;

public record NotificationEvent(
        String eventId,
        NotificationType type,
        Instant occurredAt,
        String recipientEmail,
        String recipientName,
        Map<String,Object> data
) {

}
