# RabbitMQ Configuration for IQScaffold

This directory contains RabbitMQ configuration files for the IQScaffold development environment.

## Files

- `rabbitmq.conf` - Main RabbitMQ server configuration
- `definitions.json` - Pre-configured exchanges, queues, and bindings

## Access

- **AMQP Port**: 5672
- **Management UI**: http://localhost:15672
- **Username**: iqscaffold
- **Password**: iqscaffold_password

## Pre-configured Resources

### Exchanges

1. **iqscaffold.events** (topic)
   - Main event exchange for application events
   - Durable, persistent across restarts

2. **iqscaffold.dlx** (topic)
   - Dead Letter Exchange for failed messages
   - Routes failed messages to the dead letter queue

### Queues

1. **iqscaffold.user.events**
   - Receives user-related events (routing key: `user.*`)
   - TTL: 24 hours
   - Dead letter routing enabled

2. **iqscaffold.notifications**
   - Receives notification events (routing key: `notification.*`)
   - TTL: 24 hours
   - Dead letter routing enabled

3. **iqscaffold.dlq**
   - Dead Letter Queue for failed messages
   - Stores messages that couldn't be processed

### Bindings

- `iqscaffold.events` → `iqscaffold.user.events` (routing key: `user.*`)
- `iqscaffold.events` → `iqscaffold.notifications` (routing key: `notification.*`)
- `iqscaffold.dlx` → `iqscaffold.dlq` (routing key: `#`)

## Usage in Spring Boot

Add the following to your `application.yml`:

```yaml
spring:
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:iqscaffold}
    password: ${SPRING_RABBITMQ_PASSWORD:iqscaffold_password}
    virtual-host: /
```

## Example: Publishing Messages

```java
@Autowired
private RabbitTemplate rabbitTemplate;

public void publishUserEvent(String eventType, Object payload) {
  rabbitTemplate.convertAndSend("iqscaffold.events", "user." + eventType, payload);
}
```

## Example: Consuming Messages

```java
@RabbitListener(queues = "iqscaffold.user.events")
public void handleUserEvent(Message message) {
  // Process user event
}
```

## Monitoring

Access the RabbitMQ Management UI at http://localhost:15672 to:

- Monitor queue depths
- View message rates
- Inspect exchanges and bindings
- Manage connections and channels
- View dead letter queue messages

## Production Notes

For production deployments:

1. Change default credentials
2. Enable TLS/SSL
3. Configure clustering for high availability
4. Adjust memory and disk limits based on load
5. Set up proper monitoring and alerting
6. Consider using RabbitMQ policies for queue management
