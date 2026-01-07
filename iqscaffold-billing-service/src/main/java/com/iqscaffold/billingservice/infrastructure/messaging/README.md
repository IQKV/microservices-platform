# Billing Service Mailer Implementation

This directory contains the complete mailer implementation for the IQ Scaffold Billing Service, providing code parity with the user service mailer architecture.

## 📧 Architecture Overview

The billing service mailer follows the same architectural patterns as the user service:

```
├── infrastructure/messaging/     # Event publishing and messaging
│   ├── MessagingService.java    # RabbitMQ message publishing
│   ├── BillingEvent.java        # Billing-specific events
│   ├── NotificationEvent.java   # Email notification events
│   └── MessagingException.java  # Messaging error handling
├── shared/                      # Core email services
│   ├── EmailOperations.java     # Email service interface
│   ├── EmailService.java        # Email implementation
│   ├── MessageService.java      # i18n message service
│   └── NotificationService.java # Business logic integration
├── config/                      # Configuration
│   ├── IqScaffoldProperties.java # Configuration properties
│   ├── MailConfig.java          # SMTP configuration
│   └── RabbitMQConfig.java      # Messaging configuration
└── payment/                     # Business integration example
    └── PaymentNotificationService.java # Payment notification handling
```

## 🔧 Key Components

### 1. MessagingService

- **Purpose**: Publishes billing events and notification events to RabbitMQ
- **Events**: Payment successful/failed/refunded, merchant onboarding, invoice generated
- **Integration**: Works with other microservices via message queues

### 2. EmailOperations & EmailService

- **Purpose**: Direct email sending with Thymeleaf templates
- **Features**: SMTP integration, template processing, error handling
- **Templates**: Merchant onboarding, payment notifications, invoice notifications

### 3. NotificationService

- **Purpose**: High-level notification orchestration
- **Features**: Combines email sending with event publishing
- **Benefits**: Single interface for all notification needs

### 4. PaymentNotificationService

- **Purpose**: Business logic integration example
- **Features**: Event-driven notifications, error resilience
- **Pattern**: Shows how to integrate notifications with payment processing

## 🚀 Usage Examples

### Basic Email Sending

```java
@Service
public class PaymentService {
    
    private final EmailOperations emailService;
    
    public void processPayment(PaymentRequest request) {
        // Process payment logic...
        
        // Send confirmation email
        emailService.sendPaymentSuccessfulEmail(
            request.getCustomerEmail(),
            request.getCustomerName(),
            payment.getId(),
            payment.getAmount(),
            payment.getCurrency(),
            "Subscription payment",
            LocalDateTime.now(),
            "Visa **** 1234",
            generateReceiptUrl(payment.getId())
        );
    }
}
```

### Event-Driven Notifications

```java
@Service
public class MerchantService {
    
    private final NotificationService notificationService;
    
    public void onboardMerchant(MerchantOnboardingRequest request) {
        // Create merchant account...
        
        // Send onboarding email and publish event
        notificationService.sendMerchantOnboardingNotification(
            merchant.getEmail(),
            merchant.getBusinessName(),
            generateOnboardingUrl(merchant.getId()),
            merchant.getTenantId()
        );
    }
}
```

### Payment Processing Integration

```java
@Component
public class PaymentEventHandler {
    
    private final PaymentNotificationService notificationService;
    
    @EventListener
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        var notificationEvent = new PaymentNotificationService.PaymentSuccessfulEvent(
            event.getPaymentId(),
            event.getCustomerEmail(),
            event.getCustomerName(),
            event.getAmount(),
            event.getCurrency(),
            event.getDescription(),
            event.getCompletedAt(),
            event.getPaymentMethod(),
            event.getReceiptUrl(),
            event.getTenantId()
        );
        
        notificationService.handlePaymentSuccessful(notificationEvent);
    }
}
```

## ⚙️ Configuration

### Application Properties

```yaml
iqscaffold:
  email:
    smtp:
      host: smtp.gmail.com
      port: 587
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
      auth: true
      starttls: true
      timeout: PT10S
    sender:
      fromEmail: billing@iqscaffold.com
      fromName: IQ Scaffold Billing
      baseUrl: https://billing.iqscaffold.com
    templates:
      merchantOnboardingTemplate: email/merchant-onboarding
      paymentSuccessfulTemplate: email/payment-successful
      paymentFailedTemplate: email/payment-failed
      paymentRefundedTemplate: email/payment-refunded
      invoiceGeneratedTemplate: email/invoice-generated
  billing:
    stripe:
      publicKey: ${STRIPE_PUBLIC_KEY}
      secretKey: ${STRIPE_SECRET_KEY}
      webhookSecret: ${STRIPE_WEBHOOK_SECRET}
      connectClientId: ${STRIPE_CONNECT_CLIENT_ID}
    notifications:
      enableEmailNotifications: true
      enableWebhookNotifications: true
      retryDelay: PT30S
      maxRetries: 3
  tenantIdHeader: X-Tenant-ID
```

### RabbitMQ Configuration

The service publishes events to these exchanges and routing keys:

- **Exchange**: `iqscaffold.events`
- **Routing Keys**:
  - `billing.payment.successful`
  - `billing.payment.failed`
  - `billing.payment.refunded`
  - `billing.merchant.onboarding`
  - `billing.invoice.generated`
  - `notification.email`

## 🔄 Event Flow

### Payment Success Flow

1. Payment processed successfully
2. `PaymentNotificationService.handlePaymentSuccessful()` called
3. Email sent via `EmailService.sendPaymentSuccessfulEmail()`
4. Event published via `MessagingService.publishPaymentSuccessful()`
5. Other services receive event for further processing

### Merchant Onboarding Flow

1. Merchant account created
2. `NotificationService.sendMerchantOnboardingNotification()` called
3. Email sent with onboarding instructions
4. Event published for audit/analytics
5. Merchant receives email with setup link

## 🧪 Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private TemplateEngine templateEngine;
    
    @Mock
    private IqScaffoldProperties properties;
    
    @InjectMocks
    private EmailService emailService;
    
    @Test
    void shouldSendPaymentSuccessfulEmail() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("<html>Payment Successful</html>");
        
        // Act
        emailService.sendPaymentSuccessfulEmail(
            "customer@example.com",
            "John Doe",
            "pay_123",
            new BigDecimal("99.99"),
            "USD",
            "Subscription",
            LocalDateTime.now(),
            "Visa **** 1234",
            "https://receipts.example.com/pay_123"
        );
        
        // Assert
        verify(mailSender).send(any(MimeMessage.class));
        verify(templateEngine).process(eq("email/payment-successful"), any(Context.class));
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class NotificationServiceIntegrationTest {
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");
    
    @Autowired
    private NotificationService notificationService;
    
    @Test
    void shouldSendMerchantOnboardingNotification() {
        // Test complete notification flow
        notificationService.sendMerchantOnboardingNotification(
            "merchant@example.com",
            "Acme Corp",
            "https://onboarding.example.com/token123",
            "tenant_123"
        );
        
        // Verify email sent and event published
        // ... assertions
    }
}
```

## 🔒 Security Considerations

### Email Security

- SMTP authentication required
- TLS/STARTTLS encryption enforced
- No sensitive data in email content
- Rate limiting on email sending

### Event Security

- Message encryption in transit
- Tenant isolation in events
- Audit logging for all notifications
- Input validation and sanitization

## 📊 Monitoring & Observability

### Metrics

- Email send success/failure rates
- Template processing times
- Queue message processing rates
- Error rates by notification type

### Logging

- Structured JSON logging
- Correlation ID tracking
- Tenant context in logs
- Performance metrics

### Health Checks

- SMTP connectivity
- RabbitMQ connectivity
- Template engine health
- Configuration validation

## 🔄 Error Handling

### Email Failures

- Retry logic with exponential backoff
- Dead letter queues for failed messages
- Fallback notification methods
- Graceful degradation

### Template Errors

- Template validation on startup
- Fallback to plain text emails
- Error logging and alerting
- Template hot-reloading support

## 🚀 Deployment

### Docker Configuration

```dockerfile
# Email service dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copy email templates
COPY src/main/resources/templates /app/templates
```

### Environment Variables

```bash
# SMTP Configuration
SMTP_USERNAME=billing@iqscaffold.com
SMTP_PASSWORD=secure_password

# Stripe Configuration
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# RabbitMQ Configuration
RABBITMQ_HOST=rabbitmq.iqscaffold.com
RABBITMQ_USERNAME=billing_service
RABBITMQ_PASSWORD=secure_password
```

## 📈 Performance Optimization

### Email Sending

- Connection pooling for SMTP
- Async email processing
- Batch email operations
- Template caching

### Message Processing

- Parallel message processing
- Message batching
- Connection pooling
- Circuit breaker patterns

---

This implementation provides complete code parity with the user service mailer while being specifically tailored for billing service requirements. It maintains the same architectural
patterns, error handling, and integration approaches for consistency across the IQ Scaffold platform.
