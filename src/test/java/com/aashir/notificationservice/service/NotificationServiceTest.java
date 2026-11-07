
package com.aashir.notificationservice.service;

import com.aashir.notificationservice.event.NotificationEvent;
import com.aashir.notificationservice.event.NotificationType;
import com.aashir.notificationservice.sender.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSender sender;

    @Mock
    private TemplateRenderer renderer;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                sender,
                renderer,
                "Forma",
                "https://forma.example.com"
        );

        when(renderer.render(anyString(), anyMap()))
                .thenReturn("<html>Email content</html>");
    }

    @Test
    void orderConfirmedUsesOrderTemplateAndConfirmedSubject() {
        NotificationEvent event = createEvent(
                NotificationType.ORDER_CONFIRMED,
                "Aashir",
                Map.of("orderNumber", "ORD-101")
        );

        notificationService.process(event);

        ArgumentCaptor<Map<String, Object>> variables =
                ArgumentCaptor.forClass(Map.class);

        verify(renderer).render(eq("email/order"), variables.capture());

        assertEquals("Aashir", variables.getValue().get("name"));
        assertEquals("Forma", variables.getValue().get("brandName"));
        assertEquals(false, variables.getValue().get("cancelled"));
        assertEquals("CONFIRMED", variables.getValue().get("status"));

        verify(sender).send(
                "aashir@example.com",
                "Order Confirmed - ORD-101",
                "<html>Email content</html>"
        );
    }

    @Test
    void orderCancelledUsesOrderTemplateAndCancelledSubject() {
        NotificationEvent event = createEvent(
                NotificationType.ORDER_CANCELLED,
                "Aashir",
                Map.of("orderNumber", "ORD-102")
        );

        notificationService.process(event);

        ArgumentCaptor<Map<String, Object>> variables =
                ArgumentCaptor.forClass(Map.class);

        verify(renderer).render(eq("email/order"), variables.capture());

        assertEquals(true, variables.getValue().get("cancelled"));
        assertEquals("CANCELLED", variables.getValue().get("status"));

        verify(sender).send(
                "aashir@example.com",
                "Order Cancelled - ORD-102",
                "<html>Email content</html>"
        );
    }

    @Test
    void userRegisteredUsesWelcomeTemplate() {
        NotificationEvent event = createEvent(
                NotificationType.USER_REGISTERED,
                "Aashir",
                Map.of()
        );

        notificationService.process(event);

        ArgumentCaptor<Map<String, Object>> variables =
                ArgumentCaptor.forClass(Map.class);

        verify(renderer).render(eq("email/welcome"), variables.capture());

        assertEquals("Aashir", variables.getValue().get("name"));
        assertEquals("Forma", variables.getValue().get("brandName"));
        assertEquals(
                "https://forma.example.com",
                variables.getValue().get("shopUrl")
        );

        verify(sender).send(
                "aashir@example.com",
                "Welcome to Forma, Aashir!",
                "<html>Email content</html>"
        );
    }

    @Test
    void passwordResetUsesPasswordResetTemplate() {
        NotificationEvent event = createEvent(
                NotificationType.PASSWORD_RESET_OTP,
                "Aashir",
                Map.of("otp", "123456")
        );

        notificationService.process(event);

        ArgumentCaptor<Map<String, Object>> variables =
                ArgumentCaptor.forClass(Map.class);

        verify(renderer).render(
                eq("email/password-reset-otp"),
                variables.capture()
        );

        assertEquals("123456", variables.getValue().get("otp"));

        verify(sender).send(
                "aashir@example.com",
                "Your password reset code",
                "<html>Email content</html>"
        );
    }

    @Test
    void nullRecipientNameAndDataUseFallbackValues() {
        NotificationEvent event = createEvent(
                NotificationType.USER_REGISTERED,
                null,
                null
        );

        notificationService.process(event);

        ArgumentCaptor<Map<String, Object>> variables =
                ArgumentCaptor.forClass(Map.class);

        verify(renderer).render(eq("email/welcome"), variables.capture());

        assertEquals("there", variables.getValue().get("name"));
        assertEquals("Forma", variables.getValue().get("brandName"));

        verify(sender).send(
                "aashir@example.com",
                "Welcome to Forma, there!",
                "<html>Email content</html>"
        );
    }

    @Test
    void existingOrderStatusIsPreserved() {
        Map<String, Object> data = new HashMap<>();
        data.put("orderNumber", "ORD-103");
        data.put("status", "PROCESSING");

        NotificationEvent event = createEvent(
                NotificationType.ORDER_CONFIRMED,
                "Aashir",
                data
        );

        notificationService.process(event);

        ArgumentCaptor<Map<String, Object>> variables =
                ArgumentCaptor.forClass(Map.class);

        verify(renderer).render(eq("email/order"), variables.capture());

        assertEquals("PROCESSING", variables.getValue().get("status"));
    }

    private NotificationEvent createEvent(
            NotificationType type,
            String recipientName,
            Map<String, Object> data
    ) {
        return new NotificationEvent(
                "event-101",
                type,
                Instant.now(),
                "aashir@example.com",
                recipientName,
                data
        );
    }
}
