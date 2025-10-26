# Gripday Auth Service

Centralized authentication and user management microservice providing JWT-based authentication, user lifecycle management, role-based access control, and email verification for account activation in the Gripday platform.

## Quick Start

### Prerequisites
- Java 21
- Docker and Docker Compose
- PostgreSQL 15+
- Redis 7+

### Local Development Setup

1. **Start dependencies:**
```bash
cd gripday-auth-service
docker-compose up -d postgres redis
```

2. **Run database migrations:**
```bash
mvn liquibase:update
```

3. **Start the service:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The service will be available at `http://localhost:8081`

### API Documentation
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8081/v3/api-docs`

## Authentication Endpoints

### User Registration
```bash
curl -X POST http://localhost:8081/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "default"
  }'
```

**Response (201 Created):**
```json
{
  "userId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "emailVerified": false,
  "tenantId": "default",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Note**: A verification email is automatically sent to the user's email address. Users must verify their email before they can log in.

### User Login
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!",
    "rememberMe": false
  }'
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800,
  "userContext": {
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"],
    "tenantId": "default"
  }
}
```

### Token Refresh
```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### User Logout
```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Logout From All Devices
```bash
curl -X POST http://localhost:8081/api/v1/auth/logout-all \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```
Revokes all refresh tokens for the authenticated user and invalidates all active sessions.

## Password Reset

### Forgot Password (Initiate)
Starts the password reset flow. Always returns 200 to avoid user enumeration.
```bash
curl -X POST http://localhost:8081/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

### Reset Password (Complete)
Resets the password using a reset token received by email.
```bash
curl -X POST http://localhost:8081/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "newPassword": "NewSecurePass123!"
  }'
```

Upon successful reset:
- User password is updated.
- All refresh tokens are revoked and all sessions invalidated.
- Reset token is invalidated.

## Email Verification Endpoints

### Email Verification
```bash
curl "http://localhost:8081/api/v1/auth/email/verify?token=550e8400-e29b-41d4-a716-446655440000"
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Email verified successfully",
  "username": "johndoe",
  "verifiedAt": "2024-01-15T10:30:00"
}
```

### Resend Verification Email
```bash
curl -X POST http://localhost:8081/api/v1/auth/email/resend \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Verification email sent successfully",
  "username": "johndoe",
  "verifiedAt": null
}
```

**Rate Limiting**: Maximum 3 verification emails per hour per user.

### Check Verification Status
```bash
curl "http://localhost:8081/api/v1/auth/email/status?email=john@example.com"
```

**Response (200 OK):**
```json
{
  "email": "john@example.com",
  "emailVerified": false,
  "registrationDate": "2024-01-15T10:30:00",
  "message": "Email verification pending. Please check your inbox."
}
```

## Configuration

### Environment Variables
```bash
# Database
GRIPDAY_DATABASE_URL=jdbc:postgresql://localhost:5432/gripday_auth
GRIPDAY_DATABASE_USERNAME=gripday
GRIPDAY_DATABASE_PASSWORD=password

# Redis
GRIPDAY_CACHE_REDIS_HOST=localhost
GRIPDAY_CACHE_REDIS_PORT=6379

# JWT
GRIPDAY_AUTH_JWT_SECRET=your-secret-key
GRIPDAY_AUTH_JWT_ACCESS_TOKEN_EXPIRY=PT15M
GRIPDAY_AUTH_JWT_REFRESH_TOKEN_EXPIRY=P7D

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@gripday.com
SMTP_PASSWORD=your-app-password
VERIFICATION_FROM_EMAIL=noreply@gripday.com
VERIFICATION_FROM_NAME=Gripday Platform
VERIFICATION_BASE_URL=https://app.gripday.com

# Password Reset Email Templates (optional overrides)
GRIPDAY_EMAIL_TEMPLATES_PASSWORD_RESET_SUBJECT="Reset your Gripday password"
GRIPDAY_EMAIL_TEMPLATES_PASSWORD_RESET_TEMPLATE="email/password-reset.html"

# Multi-tenant
GRIPDAY_TENANT_DEFAULT_ID=default
```

### Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service

# Stop services
docker-compose down
```

## Health Checks
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Info: `GET /actuator/info`

## Troubleshooting

### Common Issues

**Database Connection Failed**
- Verify PostgreSQL is running: `docker-compose ps postgres`
- Check connection settings in `application-local.yml`

**JWT Token Invalid**
- Ensure JWT secret is configured
- Check token expiration times
- Verify token format in Authorization header

**Email Verification Required**
- New users must verify their email before login
- Check verification email in inbox/spam folder
- Use resend endpoint if email was not received

**Email Sending Failed**
- Verify SMTP configuration and credentials
- Check SMTP server connectivity
- Ensure from-email is authorized to send

**Redis Connection Failed**
- Verify Redis is running: `docker-compose ps redis`
- Check Redis connection settings

### Logs
```bash
# View application logs
docker-compose logs -f auth-service

# View database logs
docker-compose logs -f postgres
```

## Email Verification Feature

### Overview
The Auth Service includes comprehensive email verification functionality to ensure users have valid email addresses and enhance account security.

### Key Features
- **Automatic Email Sending**: Verification emails sent immediately after user registration
- **Secure Tokens**: UUID-based tokens with 24-hour expiration
- **Rate Limiting**: Maximum 3 verification emails per hour per user
- **Single-Use Tokens**: Tokens are invalidated after successful verification
- **Multi-Tenant Support**: Verification tokens respect tenant boundaries
- **Template-Based Emails**: Professional HTML email templates with branding

### Email Verification Flow
1. User registers with email address
2. System generates secure verification token
3. Verification email sent with activation link
4. User clicks link to verify email
5. Account is activated and user can log in

### Configuration
Email verification requires SMTP configuration:

```yaml
gripday:
  email:
    smtp:
      host: smtp.gmail.com
      port: 587
      username: noreply@gripday.com
      password: your-app-password
      auth: true
      starttls: true
    verification:
      from-email: noreply@gripday.com
      from-name: Gripday Platform
      base-url: https://app.gripday.com
      token-expiry: PT24H
      rate-limit: 3
    templates:
      password-reset-subject: "Reset your Gripday password"
      password-reset-template: "email/password-reset.html"
```

### Security Features
- **Token Expiration**: Tokens expire after 24 hours
- **Single Use**: Tokens cannot be reused after verification
- **Rate Limiting**: Prevents email spam and abuse
- **Tenant Isolation**: Tokens are tenant-aware
- **HTTPS Links**: Verification links use HTTPS in production

### Password Reset Flow
1. User requests password reset via `POST /api/v1/auth/forgot-password`.
2. System generates a single-use reset token (30-minute default TTL) and sends an email with a reset link.
3. User submits `POST /api/v1/auth/reset-password` with token and new password.
4. Service updates password, revokes all refresh tokens, and invalidates sessions.

### Monitoring
- **Metrics**: Email sending success/failure rates
- **Logging**: Structured logs for verification events
- **Cleanup**: Automatic cleanup of expired tokens

## Development

### Build
```bash
mvn clean package
```

### Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn verify
```

### Code Quality
```bash
mvn clean compile -Pcode-quality
```