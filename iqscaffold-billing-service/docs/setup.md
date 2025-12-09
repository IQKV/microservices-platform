# Billing Service Setup Guide

## Overview

This guide provides step-by-step instructions for setting up the Billing Service development environment.

## Prerequisites

### Required Software

- **Java 21**: OpenJDK 21 or later
- **Maven 3.9+**: Build tool
- **Docker 24+**: For running infrastructure services
- **Docker Compose**: For local development
- **PostgreSQL 15+**: Database (via Docker)
- **Redis 7+**: Caching (via Docker)
- **RabbitMQ 3.12+**: Message broker (via Docker)
- **Git**: Version control

### Optional Tools

- **IntelliJ IDEA**: Recommended IDE
- **Postman**: API testing
- **pgAdmin**: Database management
- **RedisInsight**: Redis management

---

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/iqscaffold/backend.git
cd backend/iqscaffold-billing-service
```

### 2. Start Infrastructure

```bash
# Start PostgreSQL, Redis, and RabbitMQ
docker-compose up -d
```

### 3. Configure Environment

```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 4. Run Database Migrations

```bash
./mvnw liquibase:update -Dspring.profiles.active=local
```

### 5. Build Application

```bash
./mvnw clean package -DskipTests
```

### 6. Run Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 7. Verify Setup

```bash
# Check health
curl http://localhost:8082/actuator/health

# View API docs
open http://localhost:8082/swagger-ui.html
```

---

## Detailed Setup

### Infrastructure Setup

#### PostgreSQL

**Using Docker Compose**:

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:15-alpine
    container_name: billing-postgres
    environment:
      POSTGRES_DB: billing_db
      POSTGRES_USER: billing_user
      POSTGRES_PASSWORD: billing_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init-user.sql:/docker-entrypoint-initdb.d/init.sql
```

**Manual Setup**:

```bash
# Create database
createdb billing_db

# Create user
psql -c "CREATE USER billing_user WITH PASSWORD 'billing_pass';"

# Grant privileges
psql -c "GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_user;"
```

#### Redis

**Using Docker Compose**:

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: billing-redis
    command: redis-server --requirepass redis_pass
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
```

**Manual Setup**:

```bash
# Install Redis
brew install redis  # macOS
apt-get install redis-server  # Ubuntu

# Start Redis
redis-server --requirepass redis_pass
```

#### RabbitMQ

**Using Docker Compose**:

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: billing-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: billing_user
      RABBITMQ_DEFAULT_PASS: billing_pass
    ports:
      - "5672:5672"
      - "15672:15672"  # Management UI
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
      - ./docker/rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
      - ./docker/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
```

**Access Management UI**: http://localhost:15672 (billing_user / billing_pass)

### Application Configuration

#### Environment Variables

Create `.env` file:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/billing_db
SPRING_DATASOURCE_USERNAME=billing_user
SPRING_DATASOURCE_PASSWORD=billing_pass

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=redis_pass

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=billing_user
SPRING_RABBITMQ_PASSWORD=billing_pass

# JWT Configuration
IQSCAFFOLD_BILLING_SECURITY_JWT_JWK_SET_URI=http://localhost:8081/.well-known/jwks.json

# Payment Providers (Test Keys)
STRIPE_API_KEY=sk_test_your_test_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_IQSCAFFOLD=DEBUG
```

#### Application Properties

The service uses profile-specific configuration:

- `application.yml`: Base configuration
- `application-local.yml`: Local development
- `application-staging.yml`: Staging environment
- `application-production.yml`: Production environment

**Local Profile** (`application-local.yml`):

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  redis:
    host: ${SPRING_REDIS_HOST}
    port: ${SPRING_REDIS_PORT}
    password: ${SPRING_REDIS_PASSWORD}

logging:
  level:
    root: INFO
    com.iqscaffold: DEBUG
    org.hibernate.SQL: DEBUG
```

### Database Setup

#### Run Migrations

```bash
# System migrations (public schema)
./mvnw liquibase:update \
  -Dliquibase.changeLogFile=db/changelog/system/master.xml \
  -Dspring.profiles.active=local

# Tenant migrations (for test tenant)
./mvnw liquibase:update \
  -Dliquibase.changeLogFile=db/changelog/tenant/master.xml \
  -Dliquibase.defaultSchemaName=tenant_test \
  -Dspring.profiles.active=local
```

#### Seed Data

```bash
# Run seed data script
psql -h localhost -U billing_user -d billing_db -f scripts/seed-data.sql
```

**Seed Data Script** (`scripts/seed-data.sql`):

```sql
-- Insert subscription plans
INSERT INTO subscription_plans (code, name, tier, base_price, currency, billing_cycle, trial_days, active)
VALUES 
  ('FREE', 'Free Plan', 'FREE', 0.00, 'USD', 'MONTHLY', 0, true),
  ('PRO', 'Professional Plan', 'PRO', 49.00, 'USD', 'MONTHLY', 14, true),
  ('ENTERPRISE', 'Enterprise Plan', 'ENTERPRISE', 199.00, 'USD', 'MONTHLY', 30, true);

-- Insert plan quotas
UPDATE subscription_plans SET quotas = '{
  "API_CALLS": 1000,
  "STORAGE_GB": 1,
  "EMAIL_SENDS": 100,
  "ACTIVE_USERS": 1
}'::jsonb WHERE code = 'FREE';

UPDATE subscription_plans SET quotas = '{
  "API_CALLS": 50000,
  "STORAGE_GB": 50,
  "EMAIL_SENDS": 10000,
  "ACTIVE_USERS": 10
}'::jsonb WHERE code = 'PRO';

UPDATE subscription_plans SET quotas = '{
  "API_CALLS": -1,
  "STORAGE_GB": -1,
  "EMAIL_SENDS": -1,
  "ACTIVE_USERS": -1
}'::jsonb WHERE code = 'ENTERPRISE';
```

### Payment Provider Setup

#### Stripe

1. **Create Stripe Account**: https://dashboard.stripe.com/register
2. **Get Test API Keys**: Dashboard → Developers → API keys
3. **Configure Webhook**:
   - Dashboard → Developers → Webhooks
   - Add endpoint: `http://localhost:8082/api/v1/webhooks/stripe`
   - Select events: `payment_intent.succeeded`, `payment_intent.failed`, `charge.refunded`
   - Copy webhook secret

4. **Update Environment**:
```bash
STRIPE_API_KEY=sk_test_your_key
STRIPE_WEBHOOK_SECRET=whsec_your_secret
```

#### PayPal

1. **Create PayPal Developer Account**: https://developer.paypal.com
2. **Create App**: Dashboard → My Apps & Credentials
3. **Get Sandbox Credentials**: Copy Client ID and Secret
4. **Update Environment**:
```bash
PAYPAL_CLIENT_ID=your_client_id
PAYPAL_CLIENT_SECRET=your_client_secret
```

### IDE Setup

#### IntelliJ IDEA

1. **Import Project**:
   - File → Open → Select `pom.xml`
   - Import as Maven project

2. **Configure JDK**:
   - File → Project Structure → Project
   - Set SDK to Java 21

3. **Enable Annotation Processing**:
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Enable annotation processing

4. **Install Plugins**:
   - Lombok
   - Spring Boot
   - Database Tools

5. **Run Configuration**:
   - Run → Edit Configurations → Add New → Spring Boot
   - Main class: `com.iqscaffold.billingservice.BillingServiceApplication`
   - VM options: `-Dspring.profiles.active=local`
   - Environment variables: Load from `.env`

#### VS Code

1. **Install Extensions**:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support

2. **Configure Launch**:

```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Billing Service",
      "request": "launch",
      "mainClass": "com.iqscaffold.billingservice.BillingServiceApplication",
      "projectName": "iqscaffold-billing-service",
      "args": "--spring.profiles.active=local",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

---

## Running Tests

### Unit Tests

```bash
# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SubscriptionServiceTest

# Run with coverage
./mvnw test jacoco:report
```

### Integration Tests

```bash
# Run integration tests
./mvnw verify -P integration-tests

# Run with Testcontainers
./mvnw verify -P integration-tests -Dspring.profiles.active=test
```

### Property-Based Tests

```bash
# Run property-based tests
./mvnw test -Dtest=*PropertyTest

# Run with more iterations
./mvnw test -Dtest=*PropertyTest -Djqwik.tries=1000
```

### Architecture Tests

```bash
# Run architecture validation tests
./mvnw test -Dtest=BillingArchitectureTest
```

---

## Troubleshooting

### Database Connection Issues

**Problem**: Cannot connect to PostgreSQL

**Solution**:
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check connection
psql -h localhost -U billing_user -d billing_db

# View logs
docker logs billing-postgres
```

### Redis Connection Issues

**Problem**: Cannot connect to Redis

**Solution**:
```bash
# Check if Redis is running
docker ps | grep redis

# Test connection
redis-cli -h localhost -p 6379 -a redis_pass ping

# View logs
docker logs billing-redis
```

### Port Already in Use

**Problem**: Port 8082 already in use

**Solution**:
```bash
# Find process using port
lsof -i :8082

# Kill process
kill -9 <PID>

# Or change port in application.yml
server:
  port: 8083
```

### Migration Failures

**Problem**: Liquibase migration fails

**Solution**:
```bash
# Check migration status
./mvnw liquibase:status

# Rollback last changeset
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1

# Clear checksums
./mvnw liquibase:clearCheckSums

# Re-run migrations
./mvnw liquibase:update
```

---

## Development Workflow

### 1. Create Feature Branch

```bash
git checkout -b feature/add-payment-retry
```

### 2. Make Changes

- Write code
- Add tests
- Update documentation

### 3. Run Tests

```bash
./mvnw clean verify
```

### 4. Check Code Quality

```bash
# Run checkstyle
./mvnw checkstyle:check

# Run spotbugs
./mvnw spotbugs:check

# Run PMD
./mvnw pmd:check
```

### 5. Commit Changes

```bash
git add .
git commit -m "feat: add payment retry logic"
```

### 6. Push and Create PR

```bash
git push origin feature/add-payment-retry
```

---

## Useful Commands

### Maven

```bash
# Clean build
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Run specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Generate dependency tree
./mvnw dependency:tree

# Update dependencies
./mvnw versions:display-dependency-updates
```

### Docker

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Rebuild images
docker-compose up -d --build

# Remove volumes
docker-compose down -v
```

### Database

```bash
# Connect to database
psql -h localhost -U billing_user -d billing_db

# Dump database
pg_dump -h localhost -U billing_user -d billing_db > backup.sql

# Restore database
psql -h localhost -U billing_user -d billing_db < backup.sql

# List tables
psql -h localhost -U billing_user -d billing_db -c "\dt"
```

---

## Next Steps

1. **Read Documentation**:
   - [API Documentation](API.md)
   - [Architecture Decision Records](adr/)
   - [Runbook](RUNBOOK.md)

2. **Explore Code**:
   - Review domain model
   - Understand service layer
   - Study test examples

3. **Try Examples**:
   - Create subscription
   - Process payment
   - Generate invoice
   - Check quota

4. **Join Team**:
   - Slack: #billing-service
   - Email: billing-team@iqscaffold.com

---

## Support

For setup help:

- **Documentation**: docs/
- **Team Slack**: #billing-service
- **Email**: billing-team@iqscaffold.com
- **Office Hours**: Tuesdays 2-3 PM EST
