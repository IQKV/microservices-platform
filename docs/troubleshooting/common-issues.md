# Troubleshooting Guide

This guide covers common issues and their solutions when working with the Gripday microservices platform.

## Service Startup Issues

### Auth Service Won't Start

**Symptoms:**
- Service fails to start with database connection errors
- Application context fails to load
- Port binding errors

**Solutions:**

1. **Database Connection Issues:**
```bash
# Check PostgreSQL container status
docker-compose ps postgres

# Verify database connectivity
docker-compose exec postgres psql -U gripday -d gripday_auth -c "SELECT 1;"

# Check database logs
docker-compose logs postgres

# Restart database if needed
docker-compose restart postgres
```

2. **Port Already in Use:**
```bash
# Check what's using port 8081
lsof -i :8081
netstat -tulpn | grep 8081

# Kill process using the port
kill -9 <PID>

# Or change port in application.yml
server:
  port: 8082
```

3. **Liquibase Migration Failures:**
```bash
# Check migration status
cd gripday-auth-service
mvn liquibase:status -Dspring.profiles.active=local

# Clear locks if stuck
mvn liquibase:releaseLocks -Dspring.profiles.active=local

# Rollback and retry
mvn liquibase:rollback -Dliquibase.rollbackCount=1
mvn liquibase:update
```

### Gateway Service Won't Start

**Symptoms:**
- Gateway service fails to connect to Auth Service
- Redis connection errors
- Route configuration issues

**Solutions:**

1. **Auth Service Connectivity:**
```bash
# Verify Auth Service is running
curl http://localhost:8081/actuator/health

# Check network connectivity
docker-compose exec gateway-service ping auth-service

# Verify service discovery
docker-compose logs gateway-service | grep "auth-service"
```

2. **Redis Connection Issues:**
```bash
# Check Redis container
docker-compose ps redis

# Test Redis connectivity
docker-compose exec redis redis-cli ping

# Check Redis logs
docker-compose logs redis

# Restart Redis if needed
docker-compose restart redis
```

## Authentication Issues

### JWT Token Problems

**Symptoms:**
- 401 Unauthorized responses
- Token validation failures
- Invalid signature errors

**Solutions:**

1. **Token Format Issues:**
```bash
# Verify token format (should have 3 parts separated by dots)
echo "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." | cut -d. -f1 | base64 -d

# Check token expiration
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/auth/validate
```

2. **JWT Secret Configuration:**
```bash
# Verify JWT secret is set
docker-compose exec auth-service env | grep JWT_SECRET

# Check if secrets match between services
docker-compose exec gateway-service env | grep JWT_SECRET
```

3. **Token Refresh Issues:**
```bash
# Test token refresh endpoint
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "your-refresh-token"}'
```

### User Registration/Login Failures

**Symptoms:**
- 409 Conflict on registration
- 401 Unauthorized on login
- Validation errors

**Solutions:**

1. **Duplicate User Registration:**
```bash
# Check if user already exists
curl -X GET "http://localhost:8081/api/v1/users/search?username=testuser" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Use different username or email
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "email": "test2@example.com",
    "password": "SecurePass123!"
  }'
```

2. **Password Validation Errors:**
```bash
# Ensure password meets requirements:
# - At least 8 characters
# - Contains uppercase letter
# - Contains lowercase letter  
# - Contains number
# - Contains special character

# Valid password example
"password": "MySecure123!"
```

## Multi-Tenant Issues

### Tenant Isolation Problems

**Symptoms:**
- Users can access other tenants' data
- Tenant context not propagated
- Cross-tenant authentication issues

**Solutions:**

1. **Verify Tenant Header:**
```bash
# Always include X-Tenant-ID header
curl -H "X-Tenant-ID: tenant-123" \
     -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/users/me
```

2. **Check JWT Tenant Claims:**
```bash
# Decode JWT to verify tenant claim
echo "$TOKEN" | cut -d. -f2 | base64 -d | jq .tenantId
```

3. **Database Tenant Isolation:**
```bash
# Verify tenant_id column in database
docker-compose exec postgres psql -U gripday -d gripday_auth \
  -c "SELECT username, tenant_id FROM users LIMIT 10;"
```

## Performance Issues

### Slow Response Times

**Symptoms:**
- High response latencies
- Timeout errors
- Circuit breaker activation

**Solutions:**

1. **Database Performance:**
```bash
# Check database connections
docker-compose exec postgres psql -U gripday -d gripday_auth \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Monitor slow queries
docker-compose exec postgres psql -U gripday -d gripday_auth \
  -c "SELECT query, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

2. **Redis Performance:**
```bash
# Check Redis memory usage
docker-compose exec redis redis-cli info memory

# Monitor Redis operations
docker-compose exec redis redis-cli monitor
```

3. **JVM Performance:**
```bash
# Check JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used
curl http://localhost:8081/actuator/metrics/jvm.gc.pause

# Adjust JVM settings if needed
JAVA_OPTS="-Xmx1g -Xms512m" docker compose up auth-service
```

### Rate Limiting Issues

**Symptoms:**
- 429 Too Many Requests errors
- Inconsistent rate limiting behavior
- Rate limits not working

**Solutions:**

1. **Check Rate Limit Configuration:**
```bash
# Verify rate limiting settings
curl http://localhost:8080/actuator/configprops | jq '.["gripday.gateway.rate-limiting"]'
```

2. **Redis Rate Limit Keys:**
```bash
# Check rate limit keys in Redis
docker-compose exec redis redis-cli keys "rate_limit:*"

# View rate limit data
docker-compose exec redis redis-cli get "rate_limit:user:123"
```

3. **Adjust Rate Limits:**
```yaml
# In application.yml
gripday:
  gateway:
    rate-limiting:
      default-requests-per-minute: 200
      burst-capacity: 50
```

## Observability Issues

### Missing Metrics

**Symptoms:**
- Prometheus metrics not available
- Grafana dashboards empty
- Tracing data missing

**Solutions:**

1. **Verify Metrics Endpoints:**
```bash
# Check Prometheus endpoints
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8080/actuator/prometheus

# Verify Prometheus is scraping
curl http://localhost:9090/api/v1/targets
```

2. **OpenTelemetry Configuration:**
```bash
# Check tracing configuration
curl http://localhost:8081/actuator/configprops | jq '.["management.tracing"]'

# Verify trace export
docker-compose logs auth-service | grep -i "trace"
```

### Log Aggregation Issues

**Symptoms:**
- Logs not appearing in centralized system
- Missing correlation IDs
- Incorrect log format

**Solutions:**

1. **Check Log Configuration:**
```bash
# Verify logging configuration
curl http://localhost:8081/actuator/loggers

# Check log format
docker-compose logs auth-service | head -5
```

2. **Correlation ID Propagation:**
```bash
# Verify correlation ID in logs
docker-compose logs gateway-service | grep -o "correlationId=[^,]*"
```

## Docker and Container Issues

### Container Startup Problems

**Symptoms:**
- Containers fail to start
- Health checks failing
- Network connectivity issues

**Solutions:**

1. **Check Container Status:**
```bash
# View container status
docker-compose ps

# Check container logs
docker-compose logs auth-service
docker-compose logs gateway-service

# Inspect container details
docker inspect gripday_auth-service_1
```

2. **Network Issues:**
```bash
# Check Docker networks
docker network ls
docker network inspect gripday_default

# Test container connectivity
docker-compose exec gateway-service ping auth-service
docker-compose exec auth-service ping postgres
```

3. **Resource Constraints:**
```bash
# Check container resource usage
docker stats

# Increase memory limits if needed
services:
  auth-service:
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
```

### Volume and Data Issues

**Symptoms:**
- Data not persisting
- Permission errors
- Volume mount failures

**Solutions:**

1. **Check Volume Mounts:**
```bash
# Verify volumes
docker volume ls
docker volume inspect gripday_postgres_data

# Check permissions
docker-compose exec postgres ls -la /var/lib/postgresql/data
```

2. **Data Persistence:**
```bash
# Backup database
docker-compose exec postgres pg_dump -U gripday gripday_auth > backup.sql

# Restore database
docker-compose exec -T postgres psql -U gripday gripday_auth < backup.sql
```

## Kubernetes Deployment Issues

### Pod Startup Problems

**Symptoms:**
- Pods stuck in Pending state
- CrashLoopBackOff errors
- ImagePullBackOff issues

**Solutions:**

1. **Check Pod Status:**
```bash
# View pod details
kubectl get pods -n gripday
kubectl describe pod auth-service-xxx -n gripday

# Check pod logs
kubectl logs auth-service-xxx -n gripday
kubectl logs gateway-service-xxx -n gripday
```

2. **Resource Issues:**
```bash
# Check node resources
kubectl top nodes
kubectl describe nodes

# Adjust resource requests/limits
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

3. **Image Issues:**
```bash
# Verify image availability
kubectl describe pod auth-service-xxx -n gripday | grep -A5 "Events:"

# Check image pull secrets
kubectl get secrets -n gripday
```

### Service Discovery Issues

**Symptoms:**
- Services can't communicate
- DNS resolution failures
- Load balancing not working

**Solutions:**

1. **Check Service Configuration:**
```bash
# Verify services
kubectl get services -n gripday
kubectl describe service auth-service -n gripday

# Test service connectivity
kubectl exec -it gateway-service-xxx -n gripday -- curl http://auth-service:8081/actuator/health
```

2. **DNS Resolution:**
```bash
# Test DNS from pod
kubectl exec -it gateway-service-xxx -n gripday -- nslookup auth-service

# Check CoreDNS
kubectl get pods -n kube-system | grep coredns
kubectl logs coredns-xxx -n kube-system
```

## Diagnostic Commands

### Health Check Commands
```bash
# Service health
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8081/actuator/health/db

# Redis connectivity  
curl http://localhost:8081/actuator/health/redis

# Detailed health information
curl http://localhost:8081/actuator/health?show-details=always
```

### Metrics and Monitoring
```bash
# Application metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8080/actuator/metrics

# JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used
curl http://localhost:8081/actuator/metrics/jvm.threads.live

# Custom metrics
curl http://localhost:8081/actuator/metrics/auth.login.attempts
curl http://localhost:8080/actuator/metrics/gateway.requests
```

### Configuration Verification
```bash
# View configuration properties
curl http://localhost:8081/actuator/configprops
curl http://localhost:8080/actuator/configprops

# Environment variables
curl http://localhost:8081/actuator/env
curl http://localhost:8080/actuator/env

# Active profiles
curl http://localhost:8081/actuator/info
```

## Getting Additional Help

### Log Collection
```bash
# Collect all service logs
mkdir -p logs
docker-compose logs auth-service > logs/auth-service.log
docker-compose logs gateway-service > logs/gateway-service.log
docker-compose logs postgres > logs/postgres.log
docker-compose logs redis > logs/redis.log

# Create diagnostic bundle
tar -czf diagnostic-$(date +%Y%m%d-%H%M%S).tar.gz logs/
```

### System Information
```bash
# System information
uname -a
docker version
docker-compose version
java -version
mvn -version

# Container information
docker-compose ps
docker stats --no-stream
```

### Support Channels
- **GitHub Issues**: Report bugs and request features
- **Documentation**: Check service-specific README files
- **Community**: GitHub Discussions for questions
- **Validation Scripts**: Run `./scripts/validate-platform.sh` for comprehensive testing