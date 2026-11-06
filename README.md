# Notification Service

A Kafka-driven email notification microservice built with Spring Boot. It consumes e-commerce events and sends transactional emails using Thymeleaf templates and SMTP, with idempotent event processing to prevent duplicate notifications.

Part of an event-driven e-commerce platform.

## Notification Events

The Notification Service listens to the `notification-events` Kafka topic and processes events published by other services in the e-commerce platform.

Based on the event type, it generates and sends transactional HTML emails using Thymeleaf templates and SMTP.

### Supported Events

| Event Type | Trigger | Email Notification |
|---|---|---|
| `ORDER_CONFIRMED` | Payment succeeds and the order is confirmed | Order confirmation email containing order items and total amount |
| `ORDER_CANCELLED` | Payment fails or expires, resulting in order cancellation | Order cancellation email containing order items and total amount |
| `USER_REGISTERED` | A new user registers | Welcome email |
| `PASSWORD_RESET_OTP` | A user requests a password reset | Password reset OTP email with expiry information |

### Event Processing

- Events are consumed asynchronously through Apache Kafka.
- The service identifies the event type and processes the corresponding notification.
- Thymeleaf templates generate dynamic HTML email content.
- SMTP handles email delivery.

The Notification Service is the dedicated owner of email delivery for the platform. Other services publish events instead of sending emails directly.
