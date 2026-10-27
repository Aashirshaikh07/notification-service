package com.aashir.notificationservice.service;

import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.sender.NotificationSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationSender sender;

    public NotificationService(NotificationSender sender) {
        this.sender = sender;
    }

    public void process(NotificationEvent event){
        String name = event.recipientName() == null ? "there" : event.recipientName();

        switch (event.type()) {
            case ORDER_CONFIRMED -> sender.send(
                    event.recipientEmail(),
                    "Your order is confirmed",
                    "<h2>Hi " + name + ",</h2>"
                            + "<p>Your order <b>#" + event.data().get("orderId") + "</b> has been confirmed.</p>"
                            + "<p>Thank you for shopping with us.</p>");
            case USER_REGISTERED ->  sender.send(
                    event.recipientEmail(),
                    "Welcome aboard",
                    "<h2>Welcome, " + name + "!</h2>"
                            + "<p>Your account has been created successfully.</p>");

            case PASSWORD_RESET_OTP -> sender.send(
                    event.recipientEmail(),
                    "Your password reset code",
                    "<h2>Hi " + name + ",</h2>"
                            + "<p>Your OTP is <b>" + event.data().get("otp") + "</b>.</p>"
                            + "<p>It expires in " + event.data().get("expiresInMinutes") + " minutes.</p>");
        }
    }
}
