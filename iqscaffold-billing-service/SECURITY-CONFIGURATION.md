# Security Configuration Guide

This document provides guidance on configuring security features for the Billing Service in production environments.

## TLS 1.3 Configuration

### Application Server (Spring Boot)

TLS 1.3 should be configured at the application server level. Add the following to `application-production.yml`:

```yaml
server:
  port: 8082
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    key-store: ${SSL_KEYSTORE_PATH:/etc/ssl/billing-service/keystore.p12}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: billing-service
```

### Reverse Proxy/Load Balancer (Recommended)

For production deployments, TLS termination is typically handled by a reverse proxy (nginx, HAProxy) or cloud load balancer (AWS ALB, GCP Load Balancer). This is the recommended approach.

**Nginx Configuration Example:**

```nginx
server {
    listen 443 ssl http2;
    server_name billing.iqscaffold.com;

    # TLS 1.3 Configuration
    ssl_protocols TLSv1.3;
    ssl_certificate /etc/ssl/certs/billing-service.crt;
    ssl_certificate_key /etc/ssl/private/billing-service.key;
    
    # Strong cipher suites
    ssl_ciphers 'TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256';
    ssl_prefer_server_ciphers off;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    location / {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**AWS Application Load Balancer:**

Configure the ALB to use TLS 1.3:
- Security Policy: `ELBSecurityPolicy-TLS13-1-2-2021-06`
- Certificate: Use AWS Certificate Manager (ACM)

**GCP Load Balancer:**

Configure the load balancer to use TLS 1.3:
- Minimum TLS Version: `TLS_1_3`
- Certificate: Use Google-managed SSL certificates

## PostgreSQL Encryption at Rest

### REQ-SEC-003: Encrypt sensitive data at rest

PostgreSQL encryption at rest should be configured at the database level.

### Option 1: Transparent Data Encryption (TDE) - PostgreSQL 15+

For PostgreSQL 15+, use built-in encryption:

```sql
-- Enable encryption for the database
ALTER DATABASE billing_db SET encryption = on;
```

### Option 2: Filesystem-Level Encryption (Recommended)

Use filesystem-level encryption for the PostgreSQL data directory:

**Linux (LUKS):**

```bash
# Create encrypted volume
cryptsetup luksFormat /dev/sdb
cryptsetup luksOpen /dev/sdb pgdata_encrypted

# Format and mount
mkfs.ext4 /dev/mapper/pgdata_encrypted
mount /dev/mapper/pgdata_encrypted /var/lib/postgresql/data
```

**AWS RDS:**

Enable encryption when creating the RDS instance:

```terraform
resource "aws_db_instance" "billing_db" {
  storage_encrypted = true
  kms_key_id       = aws_kms_key.rds_key.arn
  # ... other configuration
}
```

**GCP Cloud SQL:**

Enable encryption when creating the Cloud SQL instance:

```terraform
resource "google_sql_database_instance" "billing_db" {
  settings {
    disk_encryption_configuration {
      kms_key_name = google_kms_crypto_key.sql_key.id
    }
  }
}
```

### Option 3: Column-Level Encryption (For Sensitive Fields)

For highly sensitive data (payment tokens, PII), use application-level encryption:

```java
@Entity
public class PaymentMethod {
    
    @Convert(converter = EncryptedStringConverter.class)
    private String tokenizedCardNumber;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String billingAddress;
}
```

Implement `EncryptedStringConverter` using AES-256-GCM:

```java
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Value("${iqscaffold.billing.encryption.key}")
    private String encryptionKey;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Encrypt using AES-256-GCM
        return encrypt(attribute, encryptionKey);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        // Decrypt using AES-256-GCM
        return decrypt(dbData, encryptionKey);
    }
}
```

## Redis Encryption

### REQ-SEC-005: Store secure tokens in Redis with encryption

### Option 1: Redis TLS/SSL (In-Transit Encryption)

Configure Redis to use TLS for all connections:

**Redis Configuration (`redis.conf`):**

```conf
# Enable TLS
tls-port 6380
port 0

# TLS certificates
tls-cert-file /etc/redis/certs/redis.crt
tls-key-file /etc/redis/certs/redis.key
tls-ca-cert-file /etc/redis/certs/ca.crt

# TLS protocols
tls-protocols "TLSv1.3"
```

**Spring Boot Configuration:**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6380
      ssl:
        enabled: true
      password: ${REDIS_PASSWORD}
```

### Option 2: Application-Level Encryption (At-Rest)

Encrypt sensitive data before storing in Redis:

```java
@Service
public class SecureRedisService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final EncryptionService encryptionService;
    
    public void setSecureValue(String key, String value, Duration ttl) {
        String encryptedValue = encryptionService.encrypt(value);
        redisTemplate.opsForValue().set(key, encryptedValue, ttl);
    }
    
    public String getSecureValue(String key) {
        String encryptedValue = redisTemplate.opsForValue().get(key);
        return encryptionService.decrypt(encryptedValue);
    }
}
```

### Option 3: AWS ElastiCache / GCP Memorystore Encryption

**AWS ElastiCache:**

```terraform
resource "aws_elasticache_replication_group" "billing_redis" {
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token_enabled        = true
  kms_key_id               = aws_kms_key.redis_key.arn
}
```

**GCP Memorystore:**

```terraform
resource "google_redis_instance" "billing_redis" {
  transit_encryption_mode = "SERVER_AUTHENTICATION"
  auth_enabled           = true
}
```

## Environment Variables

Set the following environment variables in production:

```bash
# JWT Configuration
USER_SERVICE_URL=https://user-service.iqscaffold.com

# Database Encryption
DB_ENCRYPTION_KEY=<base64-encoded-256-bit-key>

# Redis Encryption
REDIS_PASSWORD=<strong-password>
REDIS_SSL_ENABLED=true

# Payment Provider Secrets (encrypted at rest by cloud provider)
STRIPE_API_KEY=<stripe-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-secret>
PAYPAL_CLIENT_ID=<paypal-client-id>
PAYPAL_CLIENT_SECRET=<paypal-client-secret>
```

## Key Management

### Encryption Key Rotation

Implement a key rotation strategy for encryption keys:

1. **Database Encryption Keys**: Rotate annually
2. **Redis Encryption Keys**: Rotate quarterly
3. **JWT Signing Keys**: Rotate monthly (handled by User Service)
4. **Payment Provider Keys**: Rotate per provider recommendations

### Key Storage

Store encryption keys securely:

**AWS Secrets Manager:**

```bash
aws secretsmanager create-secret \
    --name billing-service/encryption-key \
    --secret-string '{"key":"<base64-encoded-key>"}'
```

**GCP Secret Manager:**

```bash
echo -n "<base64-encoded-key>" | \
    gcloud secrets create billing-encryption-key --data-file=-
```

**HashiCorp Vault:**

```bash
vault kv put secret/billing-service/encryption \
    key=<base64-encoded-key>
```

## Compliance Checklist

- [ ] **REQ-SEC-001**: PCI DSS compliance through payment provider abstraction
- [ ] **REQ-SEC-002**: No full credit card numbers stored (tokenization only)
- [ ] **REQ-SEC-003**: Sensitive data encrypted at rest (PostgreSQL)
- [ ] **REQ-SEC-004**: All data in transit encrypted using TLS 1.3
- [ ] **REQ-SEC-005**: Secure tokens in Redis encrypted
- [ ] **REQ-SEC-006**: Role-based access control (RBAC) implemented
- [ ] **REQ-SEC-007**: Tenant isolation enforced (schema-per-tenant)
- [ ] **REQ-SEC-008**: JWT authentication for all API requests
- [ ] **REQ-SEC-009**: Webhook signature verification implemented
- [ ] **REQ-SEC-010**: Rate limiting on public endpoints enabled

## Monitoring and Auditing

### Security Monitoring

Monitor security events:

```yaml
logging:
  level:
    com.iqscaffold.billingservice.security: INFO
    org.springframework.security: INFO
```

### Audit Logging

All security events are logged with structured logging:

- Authentication attempts (success/failure)
- Authorization failures
- Rate limit violations
- Webhook signature verification failures
- Encryption/decryption errors

### Metrics

Monitor security metrics:

- `security.authentication.success`
- `security.authentication.failure`
- `security.rate_limit.exceeded`
- `security.webhook.signature_invalid`

## Testing Security Configuration

### Verify TLS 1.3

```bash
# Test TLS version
openssl s_client -connect billing.iqscaffold.com:443 -tls1_3

# Verify cipher suites
nmap --script ssl-enum-ciphers -p 443 billing.iqscaffold.com
```

### Verify Rate Limiting

```bash
# Send multiple requests to trigger rate limit
for i in {1..150}; do
  curl -i https://billing.iqscaffold.com/api/v1/billing/plans
done
```

### Verify JWT Authentication

```bash
# Request without token (should fail)
curl -i https://billing.iqscaffold.com/api/v1/billing/portal/dashboard

# Request with valid token (should succeed)
curl -i -H "Authorization: Bearer <jwt-token>" \
  https://billing.iqscaffold.com/api/v1/billing/portal/dashboard
```

### Verify Authority-Based Access Control

```bash
# Admin endpoint with USER authority (should fail with 403)
curl -i -H "Authorization: Bearer <user-token>" \
  https://billing.iqscaffold.com/api/v1/admin/billing/analytics

# Admin endpoint with ADMIN authority (should succeed)
curl -i -H "Authorization: Bearer <admin-token>" \
  https://billing.iqscaffold.com/api/v1/admin/billing/analytics
```

## References

- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [PCI DSS Requirements](https://www.pcisecuritystandards.org/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [PostgreSQL Encryption](https://www.postgresql.org/docs/current/encryption-options.html)
- [Redis Security](https://redis.io/docs/management/security/)
