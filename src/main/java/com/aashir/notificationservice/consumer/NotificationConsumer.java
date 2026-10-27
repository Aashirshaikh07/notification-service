package com.aashir.notificationservice.consumer;

import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    private static final Logger log =  LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "notification-events",groupId = "notification-group")
    public void consume(NotificationEvent event) {
        log.info("Received notification event id ={},type={},to={}",
                event.eventId(),event.type(),event.recipientEmail());
        notificationService.process(event);
    }
}
