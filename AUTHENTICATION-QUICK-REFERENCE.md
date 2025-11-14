# Authentication Quick Reference Card

## 🔑 Key Concepts

| Concept | Description |
|---------|-------------|
| **Algorithm** | RSA256 (asymmetric) everywhere |
| **Key Distribution** | JWK endpoint at `/.well-known/jwks.json` |
| **Key Rotation** | Automatic every 90 days, 7-day grace period |
| **Token Expiry** | Access: 15 min, Refresh: 7 days |
| **Token Cleanup** | Daily at 2 AM (automatic) |

---

## 📋 Configuration Cheat Sheet

### User Service (No Configuration Needed)
```yaml
# Everything is automatic!
# - Key generation on startup
# - Key rotation every 90 days
# - Token cleanup daily at 2 AM
# - JWK endpoint at /.well-known/jwks.json
```

### Gateway Service
```yaml
gripday:
  gateway:
    security:
      jwt:
        algorithm: RS256
        issuer: gripday-user-service
        jwk-set-uri: http://user-service:8080/.well-known/jwks.json
      public-paths:
        - /.well-known/jwks.json
        - /api/v1/auth/login
        - /api/v1/auth/signup
```

### Downstream Services
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8080/.well-known/jwks.json
```

---

## 🚀 Quick Start

### 1. Test JWK Endpoint
```bash
curl http://localhost:8080/.well-known/jwks.json | jq
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}' | jq
```

### 3. Use Token
```bash
TOKEN="your-access-token"
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/protected-resource
```

---

## 🔧 Admin Operations

### Manual Key Rotation
```bash
curl -X POST http://localhost:8080/api/v1/admin/keys/rotate \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Manual Token Cleanup
```bash
curl -X POST http://localhost:8080/api/v1/admin/keys/cleanup \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Check Metrics
```bash
curl http://localhost:8080/actuator/prometheus | grep -E "jwt|token_cleanup"
```

---

## 📊 Key Metrics

```prometheus
# Authentication
http_server_requests_seconds_count{uri="/api/v1/auth/login"}

# JWK Endpoint
http_server_requests_seconds_count{uri="/.well-known/jwks.json"}

# Key Rotation
jwt_key_rotation_total
jwt_active_keys_count

# Token Cleanup
token_cleanup_refresh_tokens_total
token_cleanup_duration_seconds
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Check token expiry, verify JWK endpoint accessible |
| JWK endpoint 404 | Verify SecurityConfig allows public access |
| Token validation fails | Check issuer matches, verify network connectivity |
| Redis memory growing | Check TokenCleanupService logs, trigger manual cleanup |

---

## 📚 Documentation Links

- [AUTHENTICATION-ARCHITECTURE.md](AUTHENTICATION-ARCHITECTURE.md) - Full architecture
- [AUTHENTICATION-MIGRATION-GUIDE.md](AUTHENTICATION-MIGRATION-GUIDE.md) - Migration steps
- [AUTHENTICATION-FIXES-IMPLEMENTATION.md](AUTHENTICATION-FIXES-IMPLEMENTATION.md) - Implementation details
- [CONFIGURATION-METADATA-UPDATES.md](CONFIGURATION-METADATA-UPDATES.md) - IDE configuration

---

## 🔐 Security Best Practices

✅ Use RSA256 everywhere (no shared secrets)  
✅ Enable automatic key rotation  
✅ Monitor JWK endpoint availability  
✅ Set up alerts for key rotation failures  
✅ Monitor Redis memory usage  
✅ Use short-lived access tokens (15 min)  
✅ Implement token blacklisting for logout  
✅ Enable security audit logging  

---

## 🌍 Environment-Specific URLs

| Environment | JWK Endpoint URL |
|-------------|------------------|
| Local | `http://localhost:8080/.well-known/jwks.json` |
| Docker | `http://user-service:8080/.well-known/jwks.json` |
| Kubernetes | `http://user-service.default.svc.cluster.local:8080/.well-known/jwks.json` |
| Production | `https://api.gripday.com/.well-known/jwks.json` |

---

## ⚡ Performance Tips

- JWK endpoint responses are cached by Spring Security (5 min default)
- Key rotation has zero downtime (7-day grace period)
- Token cleanup runs during low-traffic period (2 AM)
- Redis connection pool properly sized (max-active: 8)

---

## 📞 Support

For issues or questions:
1. Check logs: `docker-compose logs -f user-service gateway-service`
2. Verify metrics: `curl http://localhost:8080/actuator/prometheus`
3. Review documentation in this repository
4. Check Redis: `redis-cli KEYS "*"`
