package com.aashir.notificationservice.sender;

public interface NotificationSender {
    void send(String to, String subject, String htmlBody);
}
