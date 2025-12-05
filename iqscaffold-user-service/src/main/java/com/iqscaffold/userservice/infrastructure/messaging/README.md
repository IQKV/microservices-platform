# Messaging Infrastructure

This package contains the RabbitMQ messaging infrastructure for the IQScaffold User Service.

## Overview

The messaging infrastructure provides asynchronous event-driven communication between services using RabbitMQ. It supports publishing and consuming events for user lifecycle operations and
notifications.

## Components

### Configuration

- **RabbitMQConfig**: Main configuration class that defines exchanges, queues, bindings, and message converters

### Events

- **UserEvent**: Event for user lifecycle operations (created, updated, deleted, verified, password reset)
- **NotificationEvent**: Event for notification requests (email, SMS, push notifications)

### Services

- **MessagingService**: Service for publishing events to RabbitMQ
- **UserEventListener**: Example listener for consuming user events

### Exceptions

- **MessagingException**: Custom exception for messaging operations

## Usage

### Publishing Events

Inject the `MessagingService` and use the convenience methods:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final MessagingService messagingService;
    
    public void createUser(User user) {
        // Create user logic...
        
        // Publish user created event
        messagingService.publishUserCreated(
            user.getId().toString(),
            user.getTenantId(),
            user.getEmail()
        );
    }
}
```

### Publishing Custom Events

For more control, create and publish events directly:

```java
UserEvent event = UserEvent.builder()
    .eventId(UUID.randomUUID().toString())
    .eventType("CUSTOM_EVENT")
    .userId(userId)
    .tenantId(tenantId)
    .email(email)
    .timestamp(Instant.now())
    .metadata(Map.of("key", "value"))
    .build();

messagingService.publishUserEvent(event, "user.custom");
```

### Publishing Notification Events

```java
NotificationEvent notification = NotificationEvent.emailNotification(
    "user@example.com",
    "John Doe",
    "Welcome to IQScaffold",
    "welcome-email",
    Map.of("name", "John", "verificationLink", "https://..."),
    tenantId,
    userId
);

messagingService.publishNotificationEvent(notification);
```

### Consuming Events

Create a listener class with `@RabbitListener` annotation:

```java
@Component
@Slf4j
public class MyEventListener {
    
    @RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE)
    public void handleUserEvent(UserEvent event) {
        log.info("Received event: {}", event.getEventType());
        // Process event...
    }
}
```

## Event Types

### User Events

- **USER_CREATED**: Published when a new user is created
- **USER_UPDATED**: Published when user information is updated
- **USER_DELETED**: Published when a user is deleted
- **USER_VERIFIED**: Published when a user verifies their email
- **PASSWORD_RESET**: Published when a user resets their password

### Notification Events

- **EMAIL**: Email notification request

## Exchanges and Queues

### Exchanges

- **iqscaffold.events**: Main topic exchange for application events
- **iqscaffold.dlx**: Dead Letter Exchange for failed messages

### Queues

- **iqscaffold.user.events**: Queue for user-related events (routing key: `user.*`)
- **iqscaffold.notifications**: Queue for notification events (routing key: `notification.*`)
- **iqscaffold.dlq**: Dead Letter Queue for failed messages

## Routing Keys

- `user.created`: User creation events
- `user.updated`: User update events
- `user.deleted`: User deletion events
- `user.verified`: User verification events
- `user.password.reset`: Password reset events
- `notification.email`: Email notification events

## Error Handling

### Retry Mechanism

Failed messages are automatically retried based on the configuration:

- Initial interval: 1 second
- Max attempts: 3
- Max interval: 10 seconds
- Multiplier: 2.0

### Dead Letter Queue

Messages that fail after all retry attempts are routed to the Dead Letter Queue (DLQ) for manual inspection and reprocessing.

## Configuration

RabbitMQ settings are configured in `application.yml` and `application-local.yml`:

```yaml
iqscaffold:
  messaging:
    rabbitmq:
      host: localhost
      port: 5672
      username: iqscaffold
      password: iqscaffold_password
      virtual-host: /
```

## Testing

For testing, use Spring AMQP Test utilities:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672"
})
class MessagingServiceTest {
    
    @Autowired
    private MessagingService messagingService;
    
    @Test
    void testPublishUserCreated() {
        messagingService.publishUserCreated("user-123", "tenant-1", "user@example.com");
        // Assert message was published...
    }
}
```

## Monitoring

Monitor RabbitMQ through:

- Management UI: http://localhost:15672
- Prometheus metrics exposed by Spring Boot Actuator
- Application logs with correlation IDs

## Best Practices

1. **Idempotency**: Ensure event handlers are idempotent as messages may be delivered multiple times
2. **Error Handling**: Always catch and log exceptions in listeners to prevent message loss
3. **Correlation IDs**: Include correlation IDs in events for distributed tracing
4. **Event Versioning**: Consider event versioning for backward compatibility
5. **Message Size**: Keep messages small; use references instead of embedding large payloads
6. **Monitoring**: Monitor queue depths and message rates to detect issues early
