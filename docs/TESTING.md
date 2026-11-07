# Testing

This document describes the automated tests currently included in the Notification Service and the areas intentionally left outside the current test scope.

## Test stack

- **JUnit 5** — test structure and assertions.
- **Mockito** — isolated unit tests for service and processing logic.
- **Spring Boot Test** — application context loading.
- **GreenMail** — an in-memory SMTP server for testing email delivery without contacting a real email provider.

Run the test suite from the project root:

```bash
mvn test
```

## Current test coverage

The current suite contains **14 test methods** across four test classes.

| Test class | Type | Coverage |
|---|---|---|
| `NotificationProcessorTest` | Unit | New event processing, skipping events already marked `SENT`, retrying an existing `FAILED` record, recording and rethrowing processing failures, rejecting blank or missing event IDs, and handling a duplicate-record insert conflict. |
| `NotificationServiceTest` | Unit | Template and subject selection for all four notification types, template variables and fallback values, and preservation of an existing order status. |
| `EmailSenderIntegrationTest` | Integration | Sends an HTML email through a local GreenMail SMTP server and verifies that the message is received with the expected recipient, subject, and HTML content. |
| `NotificationServiceApplicationTests` | Application context | Verifies that the Spring Boot application context starts successfully. |

### What the tests establish

- The processor's main idempotency and status-transition decisions are covered with mocked repository and notification-service dependencies.
- Notification type selection and the data passed to the renderer and sender are covered with Mockito.
- The email-sending adapter is tested against a local SMTP server. This does **not** send an email through Gmail or another external SMTP provider.
- The application context test checks startup wiring; it is not an end-to-end messaging test.

## Intentionally deferred testing

The following areas are not covered by the current automated suite. They are intentionally deferred rather than reported as passing tests.

| Deferred area | Current status | What a future test would verify |
|---|---|---|
| JPA repository integration | Not tested | Repository queries, entity mapping, generated schema, and the unique `event_id` constraint against a test database. |
| Kafka consumer integration | Not tested | A real or embedded Kafka broker delivering a serialized notification event to `NotificationConsumer`. |
| Kafka retry and DLT integration | Not tested | Retry/backoff behaviour, non-retryable exceptions, and publishing exhausted messages to `notification-events.DLT`. |
| Full Kafka-to-email workflow | Not tested | A message published to Kafka being consumed, persisted, rendered, sent through SMTP, and marked `SENT` as one integrated flow. |

These gaps mean the current suite validates important processing and email-sending components, but it does not prove the complete infrastructure workflow under a running Kafka broker and database.

## Testing boundaries

- The GreenMail test uses a local test SMTP server; external SMTP credentials are not required for that test.
- Mockito-based unit tests do not exercise MySQL, Kafka, or the actual JPA repository implementation.
- Retry and dead-letter configuration is documented in the main README, but its broker-level behaviour has not been integration-tested.
- No claim of exactly-once email delivery or complete end-to-end test coverage is made.

