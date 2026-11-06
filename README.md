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

## How It Works

The Notification Service processes each incoming Kafka event through a pipeline of event consumption, database-backed idempotency checks, template rendering, and SMTP email delivery.

### Processing Flow

1. **Event publishing:** Upstream services publish a `NotificationEvent` to the `notification-events` Kafka topic.
2. **Event consumption:** `NotificationConsumer`, running in the `notification-group` consumer group, receives the event and passes it to `NotificationProcessor`.
3. **Event validation and lookup:** `NotificationProcessor` validates that the event has an `eventId` and looks up its record in the `notification_log` table.
4. **Idempotency check:**
   - If the event already has status `SENT`, processing is skipped.
   - If no record exists, a new `PENDING` record is created.
   - If a record is already `PENDING` or `FAILED`, the existing record is reused.
5. **Notification preparation:** The attempt counter is incremented. `NotificationService` selects the email subject and Thymeleaf template based on the event type.
6. **Template rendering:** `TemplateRenderer` processes the selected template with event data and shared variables to produce the HTML email body.
7. **Email delivery:** `EmailSender` uses Spring's `JavaMailSender` to send the HTML email through the configured SMTP server.
8. **Status update:** On success, the log is marked `SENT` and its previous error message is cleared. On a runtime failure, the log is marked `FAILED`, the error is stored, and the exception is rethrown so Kafka's error handler can handle the failed record.

### Architecture Flow

![Notification Processing Architecture](docs/images/notification-processing-flow.svg)

### Database-backed Idempotency

Each notification event is tracked by its `eventId`. The `notification_log` table enforces a unique constraint on this field, while the processor checks the stored status before sending an email. This prevents an event already marked `SENT` from being processed again and allows existing `PENDING` or `FAILED` records to be reused during another processing attempt.

This is idempotent event processing, not a guarantee of exactly-once email delivery. For example, an SMTP send may succeed while the subsequent database status update fails, which can cause another send attempt.

### Retry and Dead-Letter Handling

For retryable processing failures, the configured Kafka `DefaultErrorHandler` uses exponential backoff: an initial interval of 2 seconds, a multiplier of 2, and a maximum interval of 10 seconds, with up to 3 retries. After retries are exhausted, the failed record is published to `notification-events.DLT`.

`IllegalArgumentException` is configured as non-retryable, so invalid events such as those missing an `eventId` are not retried.
