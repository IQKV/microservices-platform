# Design Document

## Overview

This design implements user email activation for the Gripday authentication system by extending the existing three-tier architecture. The solution adds email verification functionality with minimal changes to the current codebase, leveraging the existing `emailVerified` field in the User entity and following established patterns for security, multi-tenancy, and observability.

## Architecture

### High-Level Flow

```
User Registration → Generate Verification Token → Send Email → User Clicks Link → Verify Token → Activate Account
```

### Integration Points

- **Auth Service**: Extended with email verification endpoints and services
- **Database**: New verification_tokens table with existing users table relationship
- **Email Service**: New service for sending verification emails (SMTP integration)
- **Gateway Service**: Routes email verification endpoints (no changes needed)

## Components and Interfaces

### 1. New Database Entity

**EmailVerificationToken Entity**

```java
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends TenantAwareEntity {

  private String token; // UUID-based secure token
  private Long userId; // Foreign key to users table
  private LocalDateTime expiresAt; // 24-hour expiration
  private LocalDateTime createdAt;
  private Boolean used; // Single-use flag
}
```

### 2. Domain Services

**EmailVerificationService** (New)

- `generateVerificationToken(User user)` - Creates secure token with 24h expiration
- `verifyEmail(String token)` - Validates token and activates user account
- `resendVerificationEmail(String email)` - Generates new token and sends email
- `cleanupExpiredTokens()` - Scheduled cleanup of expired tokens

**EmailService** (New)

- `sendVerificationEmail(User user, String token)` - Sends HTML verification email
- `buildVerificationUrl(String token)` - Constructs verification link
- Template-based email generation with branding

**AuthenticationService** (Modified)

- Enhanced `authenticateUser()` to check `emailVerified` status
- Returns specific error for unverified accounts

**UserRegistrationService** (Modified)

- Triggers email verification after successful registration
- Sets `emailVerified = false` by default

### 3. Presentation Layer

**EmailVerificationResource** (New)

```java
@RestController
@RequestMapping("/api/v1/auth/email")
public class EmailVerificationResource {

  @GetMapping("/verify")
  public ResponseEntity<EmailVerificationResponse> verifyEmail(@RequestParam String token);

  @PostMapping("/resend")
  public ResponseEntity<ResendVerificationResponse> resendVerification(@RequestBody ResendRequest request);

  @GetMapping("/status")
  public ResponseEntity<VerificationStatusResponse> getVerificationStatus(@RequestParam String email);
}
```

**AuthenticationResource** (Modified)

- Enhanced error responses for unverified accounts
- Updated login flow to check email verification

### 4. Infrastructure Layer

**EmailVerificationTokenRepository** (New)

```java
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
  Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);
  List<EmailVerificationToken> findByUserIdAndUsedFalse(Long userId);
  void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
```

## Data Models

### Database Schema Changes

**New Liquibase Migration: 007-create-email-verification-tokens-table.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <changeSet id="7" author="gripday">
        <comment>Create email_verification_tokens table for user email activation</comment>

        <createTable tableName="email_verification_tokens">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="token" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="expires_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="used" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="email_verification_tokens"
            baseColumnNames="user_id"
            referencedTableName="users"
            referencedColumnNames="id"
            constraintName="fk_email_verification_tokens_user_id"/>

        <createIndex tableName="email_verification_tokens" indexName="idx_email_verification_token">
            <column name="token"/>
        </createIndex>

        <createIndex tableName="email_verification_tokens" indexName="idx_email_verification_user_id">
            <column name="user_id"/>
        </createIndex>

        <createIndex tableName="email_verification_tokens" indexName="idx_email_verification_expires_at">
            <column name="expires_at"/>
        </createIndex>

        <rollback>
            <dropTable tableName="email_verification_tokens"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Update db.changelog-master.xml**
Add the new migration file to the master changelog:

```xml
<include file="007-create-email-verification-tokens-table.xml" relativeToChangelogFile="true"/>
```

### DTOs and Request/Response Models

**EmailVerificationResponse**

```java
public record EmailVerificationResponse(boolean success, String message, String username, LocalDateTime verifiedAt) {}
```

**ResendVerificationRequest**

```java
public record ResendVerificationRequest(@Email @NotBlank String email) {}
```

**VerificationStatusResponse**

```java
public record VerificationStatusResponse(String email, boolean emailVerified, LocalDateTime registrationDate, String message) {}
```

## Error Handling

### New Error Codes

- `EMAIL_VERIFICATION_REQUIRED` - User must verify email before login
- `VERIFICATION_TOKEN_INVALID` - Token not found or already used
- `VERIFICATION_TOKEN_EXPIRED` - Token has expired (>24 hours)
- `EMAIL_RESEND_RATE_LIMITED` - Too many resend requests (>3 per hour)
- `EMAIL_ALREADY_VERIFIED` - Email is already verified
- `EMAIL_SEND_FAILED` - SMTP or email service failure

### Enhanced Login Error Response

```json
{
  "error": {
    "code": "EMAIL_VERIFICATION_REQUIRED",
    "message": "Email verification required",
    "details": "Please check your email and click the verification link to activate your account",
    "actions": {
      "resendEmail": "/api/v1/auth/email/resend"
    }
  }
}
```

## Testing Strategy

### Unit Tests

- **EmailVerificationService**: Token generation, validation, expiration logic
- **EmailService**: Email template generation and SMTP integration
- **EmailVerificationResource**: Endpoint validation and error handling
- **Repository**: Database operations and query validation

### Integration Tests

- **Email Verification Flow**: End-to-end registration → email → verification
- **Rate Limiting**: Resend email rate limiting functionality
- **Token Expiration**: Expired token handling and cleanup
- **Multi-tenant**: Tenant isolation for verification tokens

### Security Tests

- **Token Security**: UUID generation and uniqueness validation
- **HTTPS Enforcement**: Verification links use HTTPS in production
- **Rate Limiting**: Email spam prevention testing
- **Token Invalidation**: Single-use token enforcement

## Configuration

### Email Service Configuration

```yaml
gripday:
  email:
    smtp:
      host: ${SMTP_HOST:localhost}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME:}
      password: ${SMTP_PASSWORD:}
      auth: true
      starttls: true
    sender:
      from-email: ${EMAIL_FROM_EMAIL:noreply@gripday.com}
      from-name: ${EMAIL_FROM_NAME:Gripday Platform}
      base-url: ${APP_BASE_URL:https://app.gripday.com}
    verification:
      token-expiry: PT24H
      rate-limit: 3 # emails per hour per user
    templates:
      verification-subject: "Verify your Gripday account"
      verification-template: "email/verification.html"
```

### Environment Variables

```bash
# SMTP Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@gripday.com
SMTP_PASSWORD=app-specific-password

# Email Sender Settings
EMAIL_FROM_EMAIL=noreply@gripday.com
EMAIL_FROM_NAME=Gripday Platform
APP_BASE_URL=https://app.gripday.com
```

## Security Considerations

### Token Security

- **UUID Generation**: Cryptographically secure random UUIDs
- **Single Use**: Tokens invalidated after successful verification
- **Time Limited**: 24-hour expiration with automatic cleanup
- **HTTPS Only**: All verification links use HTTPS in production

### Rate Limiting

- **Email Resend**: Maximum 3 emails per hour per user
- **Redis Integration**: Leverage existing Redis for rate limiting
- **IP-based Limits**: Additional protection against abuse

### Data Protection

- **Tenant Isolation**: Verification tokens respect multi-tenant boundaries
- **Audit Logging**: All verification events logged for security monitoring
- **PII Handling**: Email addresses handled according to privacy standards

## Performance Considerations

### Database Optimization

- **Indexes**: Optimized queries on token, user_id, and expires_at columns
- **Cleanup Job**: Scheduled task to remove expired tokens (daily at 2 AM)
- **Connection Pooling**: Leverage existing database connection management

### Email Delivery

- **Async Processing**: Email sending handled asynchronously to avoid blocking
- **Retry Logic**: Failed email delivery retry with exponential backoff
- **Template Caching**: Email templates cached for performance

### Monitoring

- **Metrics**: Email delivery success/failure rates
- **Alerts**: Failed email delivery notifications
- **Performance**: Email sending duration tracking

## Observability Integration

### Metrics

- `gripday_auth_email_verification_sent_total` - Total verification emails sent
- `gripday_auth_email_verification_success_total` - Successful verifications
- `gripday_auth_email_verification_expired_total` - Expired token attempts
- `gripday_auth_email_send_duration_seconds` - Email sending duration

### Logging

- **Structured Logs**: JSON format with correlation IDs
- **Security Events**: Failed verification attempts logged
- **Performance Logs**: Email delivery timing and status

### Tracing

- **OpenTelemetry**: Distributed tracing for email verification flow
- **Span Tags**: User ID, tenant ID, verification status
- **Error Tracking**: Failed verification attempts with context
