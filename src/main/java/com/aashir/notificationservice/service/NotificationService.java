package com.aashir.notificationservice.service;

import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.sender.NotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationSender sender;
    private final TemplateRenderer renderer;
    private final String brandName;
    private final String shopUrl;

    public NotificationService(NotificationSender sender, TemplateRenderer renderer,
                               @Value("${app.brand-name}") String brandName,
                               @Value("${app.shop-url}") String shopUrl) {
        this.sender = sender;
        this.renderer = renderer;
        this.brandName = brandName;
        this.shopUrl = shopUrl;
    }

    public void process(NotificationEvent event){
        Map<String,Object> data = event.data() == null ? Map.of() : event.data();
        String name = event.recipientName() == null ? "there" : event.recipientName();

        Map<String,Object> vars = new HashMap<>(data);
        vars.put("brandName", brandName);
        vars.put("name", name);

        String template;
        String subject;

        switch (event.type()) {
            case ORDER_CONFIRMED -> {
                vars.put("cancelled", false);
                vars.putIfAbsent("status", "CONFIRMED");
                template = "email/order";
                subject = "Order Confirmed - " + data.get("orderNumber");
            }
            case ORDER_CANCELLED -> {
                vars.put("cancelled", true);
                vars.putIfAbsent("status", "CANCELLED");
                template = "email/order";
                subject = "Order Cancelled - " + data.get("orderNumber");
            }
            case USER_REGISTERED -> {
                vars.put("shopUrl", shopUrl);
                template = "email/welcome";
                subject = "Welcome to " + brandName + ", " + name + "!";
            }
            case PASSWORD_RESET_OTP -> {
                template = "email/password-reset-otp";
                subject = "Your password reset code";
            }
            default -> throw new IllegalArgumentException("Unsupported notification type: " + event.type());
        }
        sender.send(event.recipientEmail(), subject, renderer.render(template, vars));

    }
}
