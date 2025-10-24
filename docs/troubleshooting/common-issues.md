# Troubleshooting Common Issues

This guide provides solutions to common issues encountered when developing, deploying, and operating the Gripday microservices platform.

## Quick Diagnostics

### Health Check Commands
```bash
# Check all services
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8080/actuator/health  # Gateway Service

# Check infrastructure
docker-compose ps                           # Container status
docker-compose logs -f postgres            # Database logs
docker-compose logs -f redis               # Redis logs
```

### Service Status Verification
```bash
# Test authentication flow
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Check gateway routing
curl http://localhost:8080/actuator/gateway/routes

# Verify database connection
psql -h localhost -U gripday -d gripday_auth -c "SELECT 1;"
```

## Authentication Issues

### JWT Token Problems

**Issue: "Invalid or expired token" (401)**

**Symptoms:**
- API calls return 401 Unauthorized
- Token validation fails
- Authentication endpoints work but protected endpoints don't

**Solutions:**
```bash
# 1. Check token format
echo "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." | cut -d' ' -f2 | base64 -d

# 2. Verify JWT secret configuration
curl http://localhost:8081/actuator/env | grep jwt.secret

# 3. Check token expiration
# Decode JWT payload (second part after first dot)
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.PAYLOAD.SIGNATURE"
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq .exp

# 4. Test with fresh token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123!"}' | jq -r '.accessToken'
```

**Configuration Fix:**
```yaml
# Ensure consistent JWT secret across services
gripday:
  auth:
    jwt:
      secret: ${GRIPDAY_AUTH_JWT_SECRET:your_secret_key_minimum_256_bits}
      access-token-expiry: PT15M
      refresh-token-expiry: P7D
```

### Account Lockout Issues

**Issue: "Account temporarily locked" (423)**

**Symptoms:**
- Login attempts return 423 status
- User cannot authenticate after multiple failed attempts

**Solutions:**
```bash
# 1. Check lockout status in database
psql -h localhost -U gripday -d gripday_auth -c "
  SELECT username, failed_attempts, locked_until 
  FROM users 
  WHERE username = 'problematic_user';"

# 2. Manually unlock account (development only)
psql -h localhost -U gripday -d gripday_auth -c "
  UPDATE users 
  SET failed_attempts = 0, locked_until = NULL 
  WHERE username = 'problematic_user';"

# 3. Check lockout configuration
curl http://localhost:8081/actuator/configprops | grep lockout
```

**Configuration Adjustment:**
```yaml
gripday:
  auth:
    account-lockout:
      attempts: 5                    # Reduce for tighter security
      duration: PT15M               # Increase for longer lockout
```

## Database Connection Issues

### PostgreSQL Connection Failed

**Issue: "Connection refused" or "Database connection failed"**

**Symptoms:**
- Service fails to start
- Database health check fails
- Liquibase migrations fail

**Diagnostic Commands:**
```bash
# 1. Check PostgreSQL container status
docker-compose ps postgres

# 2. Test direct connection
psql -h localhost -p 5432 -U gripday -d gripday_auth

# 3. Check PostgreSQL logs
docker-compose logs postgres | tail -50

# 4. Verify network connectivity
docker network ls
docker network inspect gripday-platform_default
```

**Solutions:**
```bash
# 1. Restart PostgreSQL container
docker-compose restart postgres

# 2. Check port availability
lsof -i :5432
netstat -tulpn | grep 5432

# 3. Verify environment variables
echo $GRIPDAY_DATABASE_URL
echo $GRIPDAY_DATABASE_USERNAME
echo $GRIPDAY_DATABASE_PASSWORD

# 4. Reset database (development only)
docker-compose down -v
docker-compose up -d postgres
```

### Liquibase Migration Issues

**Issue: Migration fails or database schema out of sync**

**Symptoms:**
- Service startup fails with migration errors
- Database tables missing or incorrect structure

**Solutions:**
```bash
# 1. Check migration status
cd gripday-auth-service
mvn liquibase:status -Dspring.profiles.active=local

# 2. View migration history
psql -h localhost -U gripday -d gripday_auth -c "
  SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;"

# 3. Force migration (development only)
mvn liquibase:update -Dspring.profiles.active=local

# 4. Rollback last migration (if needed)
mvn liquibase:rollback -Dliquibase.rollbackCount=1 -Dspring.profiles.active=local

# 5. Clear migration history and restart (development only)
psql -h localhost -U gripday -d gripday_auth -c "
  DROP TABLE IF EXISTS databasechangelog;
  DROP TABLE IF EXISTS databasechangeloglock;"
mvn liquibase:update -Dspring.profiles.active=local
```

## Redis Connection Issues

### Redis Connection Failed

**Issue: "Unable to connect to Redis" or caching not working**

**Symptoms:**
- Rate limiting not working
- Session management fails
- Cache-related errors in logs

**Diagnostic Commands:**
```bash
# 1. Check Redis container status
docker-compose ps redis

# 2. Test Redis connection
redis-cli -h localhost -p 6379 ping

# 3. Check Redis logs
docker-compose logs redis | tail -20

# 4. Monitor Redis operations
redis-cli -h localhost -p 6379 monitor
```

**Solutions:**
```bash
# 1. Restart Redis container
docker-compose restart redis

# 2. Check Redis configuration
redis-cli -h localhost -p 6379 config get "*"

# 3. Clear Redis data (development only)
redis-cli -h localhost -p 6379 flushall

# 4. Verify Redis connectivity from application
curl http://localhost:8081/actuator/health/redis
```

## Gateway Service Issues

### Service Routing Problems

**Issue: 404 Not Found or routing not working**

**Symptoms:**
- Requests to gateway return 404
- Services not reachable through gateway
- Route configuration not working

**Diagnostic Commands:**
```bash
# 1. Check gateway routes
curl http://localhost:8080/actuator/gateway/routes | jq .

# 2. Check service discovery
curl http://localhost:8080/actuator/gateway/globalfilters

# 3. Test direct service access
curl http://localhost:8081/api/v1/auth/health  # Direct auth service
curl http://localhost:8080/api/v1/auth/health  # Through gateway

# 4. Check gateway logs
docker-compose logs gateway-service | grep -i error
```

**Solutions:**
```bash
# 1. Verify route configuration
curl http://localhost:8080/actuator/env | grep -i route

# 2. Check backend service health
curl http://localhost:8081/actuator/health

# 3. Restart gateway service
docker-compose restart gateway-service

# 4. Update route configuration
# Edit application-local.yml and restart service
```

### Rate Limiting Issues

**Issue: Rate limiting not working or too restrictive**

**Symptoms:**
- 429 Too Many Requests errors
- Rate limiting headers missing
- Rate limits not enforced

**Solutions:**
```bash
# 1. Check rate limiting configuration
curl http://localhost:8080/actuator/configprops | grep -i rate

# 2. Monitor Redis rate limiting keys
redis-cli -h localhost -p 6379 keys "*rate*"
redis-cli -h localhost -p 6379 get "request_rate_limiter.{user_id}.tokens"

# 3. Test rate limiting
for i in {1..10}; do
  curl -w "%{http_code}\n" -o /dev/null -s http://localhost:8080/api/v1/auth/health
done

# 4. Adjust rate limiting configuration
# Update application-local.yml:
gripday:
  gateway:
    rate-limiting:
      default-requests-per-minute: 200  # Increase limit
      burst-capacity: 50               # Increase burst
```

## Circuit Breaker Issues

**Issue: Circuit breaker stuck open or not working**

**Symptoms:**
- 503 Service Unavailable errors
- Circuit breaker always open
- Fallback responses not working

**Solutions:**
```bash
# 1. Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# 2. Check circuit breaker metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls

# 3. Force circuit breaker state (development only)
curl -X POST http://localhost:8080/actuator/circuitbreakers/auth-service/state \
  -H "Content-Type: application/json" \
  -d '{"state":"CLOSED"}'

# 4. Adjust circuit breaker configuration
# Update application-local.yml:
resilience4j:
  circuitbreaker:
    instances:
      auth-service:
        failure-rate-threshold: 70      # Increase threshold
        wait-duration-in-open-state: PT10S  # Reduce wait time
```

## Performance Issues

### High Memory Usage

**Issue: Services consuming excessive memory**

**Symptoms:**
- OutOfMemoryError exceptions
- Slow response times
- Container restarts

**Diagnostic Commands:**
```bash
# 1. Check JVM memory usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used
curl http://localhost:8081/actuator/metrics/jvm.memory.max

# 2. Generate heap dump (development only)
jcmd <PID> GC.run_finalization
jcmd <PID> VM.gc
jmap -dump:format=b,file=heapdump.hprof <PID>

# 3. Check container memory usage
docker stats

# 4. Monitor garbage collection
curl http://localhost:8081/actuator/metrics/jvm.gc.pause
```

**Solutions:**
```bash
# 1. Adjust JVM memory settings
export JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"
mvn spring-boot:run

# 2. Update Docker memory limits
# In docker-compose.yml:
services:
  auth-service:
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

# 3. Optimize database connection pool
# In application.yml:
gripday:
  database:
    hikari:
      maximum-pool-size: 10    # Reduce pool size
      minimum-idle: 2          # Reduce idle connections
```

### Slow Database Queries

**Issue: Database queries taking too long**

**Symptoms:**
- Slow API response times
- Database connection pool exhaustion
- Timeout errors

**Solutions:**
```bash
# 1. Enable query logging
# Add to application-local.yml:
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# 2. Check slow queries in PostgreSQL
psql -h localhost -U gripday -d gripday_auth -c "
  SELECT query, mean_exec_time, calls 
  FROM pg_stat_statements 
  ORDER BY mean_exec_time DESC 
  LIMIT 10;"

# 3. Analyze query execution plans
psql -h localhost -U gripday -d gripday_auth -c "
  EXPLAIN ANALYZE SELECT * FROM users WHERE username = 'testuser';"

# 4. Add missing indexes
psql -h localhost -U gripday -d gripday_auth -c "
  CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
  CREATE INDEX CONCURRENTLY idx_users_tenant_id ON users(tenant_id);"
```

## Development Environment Issues

### Port Conflicts

**Issue: "Port already in use" errors**

**Solutions:**
```bash
# 1. Find process using port
lsof -i :8080
lsof -i :8081

# 2. Kill process using port
kill -9 <PID>

# 3. Use different ports
mvn spring-boot:run -Dserver.port=8082

# 4. Check Docker port mappings
docker-compose ps
docker port <container_name>
```

### Maven Build Issues

**Issue: Build failures or dependency conflicts**

**Solutions:**
```bash
# 1. Clean and rebuild
mvn clean install

# 2. Update dependencies
mvn dependency:resolve
mvn versions:display-dependency-updates

# 3. Check for conflicts
mvn dependency:tree
mvn dependency:analyze

# 4. Clear Maven cache
rm -rf ~/.m2/repository
mvn clean install

# 5. Skip tests for faster builds
mvn clean package -DskipTests
```

### IDE Configuration Issues

**Issue: IDE not recognizing Java 21 features or Spring Boot configuration**

**Solutions:**

**IntelliJ IDEA:**
```bash
# 1. Set Project SDK to Java 21
File → Project Structure → Project → Project SDK

# 2. Enable annotation processing
File → Settings → Build → Compiler → Annotation Processors → Enable

# 3. Refresh Maven project
Maven tool window → Reload All Maven Projects

# 4. Invalidate caches
File → Invalidate Caches and Restart
```

**VS Code:**
```bash
# 1. Install required extensions
code --install-extension vscjava.vscode-java-pack
code --install-extension pivotal.vscode-spring-boot

# 2. Configure Java home
# Add to settings.json:
{
  "java.home": "/path/to/java-21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java-21"
    }
  ]
}
```

## Monitoring and Logging

### Missing Logs or Metrics

**Issue: Logs not appearing or metrics not collected**

**Solutions:**
```bash
# 1. Check logging configuration
curl http://localhost:8081/actuator/loggers

# 2. Adjust log levels
curl -X POST http://localhost:8081/actuator/loggers/org.gripday \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 3. Check metrics endpoints
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/prometheus

# 4. Verify observability configuration
curl http://localhost:8081/actuator/env | grep -i otel
```

## Getting Help

### Diagnostic Information Collection

When reporting issues, collect the following information:

```bash
# 1. Service versions and health
curl http://localhost:8081/actuator/info
curl http://localhost:8080/actuator/info
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health

# 2. Configuration
curl http://localhost:8081/actuator/env > auth-service-env.json
curl http://localhost:8080/actuator/env > gateway-service-env.json

# 3. Recent logs
docker-compose logs --tail=100 auth-service > auth-service.log
docker-compose logs --tail=100 gateway-service > gateway-service.log

# 4. System information
docker-compose ps > containers-status.txt
docker system df > docker-usage.txt
```

### Support Channels

- **Documentation**: Check service-specific README files
- **API Documentation**: Swagger UI at `http://localhost:8081/swagger-ui.html`
- **Logs**: Use `docker-compose logs -f <service-name>` for real-time logs
- **Metrics**: Monitor via `/actuator/metrics` endpoints

### Emergency Procedures

**Complete Environment Reset (Development Only):**
```bash
# Stop all services
docker-compose down -v

# Clean Docker system
docker system prune -f
docker volume prune -f

# Rebuild and restart
mvn clean package
docker-compose up -d --build

# Verify services
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health
```