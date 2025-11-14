# Gateway Routing Fix - Validation Checklist

## Pre-Deployment Validation

### ✅ Code Changes
- [x] Gateway route configuration updated to include all auth-service paths
- [x] Public paths configuration updated with password management endpoints
- [x] Rate limiting configuration added for all new endpoints
- [x] Local development configuration updated
- [x] No compilation errors or diagnostics

### ✅ Configuration Files Modified
- [x] `GatewayConfig.java` - Route paths expanded
- [x] `application.yml` - Public paths and rate limits updated
- [x] `application-local.yml` - Development configuration updated

---

## Post-Deployment Testing

### 1. Public Endpoints (No Auth) ✓

#### Authentication
```bash
# Should return 201 Created
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"Test123!","firstName":"Test","lastName":"User"}'

# Should return 200 OK with tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"test","password":"Test123!"}'

# Should return 200 OK with validation result
curl -X POST http://localhost:8080/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d '{"token":"your-jwt-token"}'
```

#### Email Verification
```bash
# Should return 200 OK or 400 Bad Request (depending on token validity)
curl -X GET "http://localhost:8080/api/v1/auth/email/verify?token=test-token"

# Should return 200 OK
curl -X POST http://localhost:8080/api/v1/auth/email/resend \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com"}'

# Should return 200 OK or 404 Not Found
curl -X GET "http://localhost:8080/api/v1/auth/email/status?email=test@test.com"
```

#### Password Management (NEW - CRITICAL)
```bash
# Should return 202 Accepted
curl -X POST http://localhost:8080/api/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com"}'

# Should return 204 No Content or 400 Bad Request
curl -X POST http://localhost:8080/api/v1/password/reset \
  -H "Content-Type: application/json" \
  -d '{"token":"reset-token","newPassword":"NewPass123!"}'
```

**Expected**: All endpoints should be accessible (not 404)

---

### 2. Authenticated Endpoints (JWT Required) ✓

#### User Profile (NEW - CRITICAL)
```bash
# Should return 200 OK with user context
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Password Change (NEW - CRITICAL)
```bash
# Should return 204 No Content or 400 Bad Request
curl -X POST http://localhost:8080/api/v1/password/change \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"Test123!","newPassword":"NewTest123!"}'
```

#### Session Management
```bash
# Should return 204 No Content
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Should return 204 No Content
curl -X POST http://localhost:8080/api/v1/auth/logout-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected**: All endpoints should be accessible and require authentication

---

### 3. Admin Endpoints (ADMIN/SUPER_ADMIN Role) ✓

#### User Management (NEW - CRITICAL)
```bash
# Should return 200 OK with paginated users or 403 Forbidden
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# Should return 200 OK or 404 Not Found
curl -X GET http://localhost:8080/api/v1/users/1 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# Should return 201 Created or 400/409
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin-created","email":"admin@test.com","password":"Test123!","firstName":"Admin","lastName":"Created","roles":["USER"]}'
```

#### Organization Management (NEW - CRITICAL)
```bash
# Should return 200 OK with paginated organizations or 403 Forbidden
curl -X GET http://localhost:8080/api/v1/organizations \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# Should return 200 OK or 404 Not Found
curl -X GET http://localhost:8080/api/v1/organizations/1 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

**Expected**: All endpoints should be accessible and enforce role requirements

---

### 4. Super Admin Endpoints (SUPER_ADMIN Role) ✓

#### Tenant Management (NEW - CRITICAL)
```bash
# Should return 200 OK with tenant list or 403 Forbidden
curl -X GET http://localhost:8080/api/v1/admin/tenants \
  -H "Authorization: Bearer SUPER_ADMIN_JWT_TOKEN"

# Should return 200 OK or 404 Not Found
curl -X GET http://localhost:8080/api/v1/admin/tenants/test-tenant \
  -H "Authorization: Bearer SUPER_ADMIN_JWT_TOKEN"

# Should return 200 OK with statistics or 403 Forbidden
curl -X GET http://localhost:8080/api/v1/admin/tenants/statistics \
  -H "Authorization: Bearer SUPER_ADMIN_JWT_TOKEN"
```

**Expected**: All endpoints should be accessible and enforce SUPER_ADMIN role

---

### 5. Rate Limiting Validation ✓

#### Test Password Reset Rate Limit (3 req/min in production)
```bash
# Run this 5 times quickly - should get 429 after 3rd request
for i in {1..5}; do
  echo "Request $i:"
  curl -w "\nHTTP Status: %{http_code}\n" \
    -X POST http://localhost:8080/api/v1/password/forgot \
    -H "Content-Type: application/json" \
    -d '{"email":"ratelimit@test.com"}'
  sleep 1
done
```

**Expected**: First 3-5 requests succeed, subsequent requests return 429 Too Many Requests

#### Test Login Rate Limit (10 req/min in production)
```bash
# Run this 15 times quickly - should get 429 after 10th request
for i in {1..15}; do
  echo "Request $i:"
  curl -w "\nHTTP Status: %{http_code}\n" \
    -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test","password":"wrong"}'
  sleep 0.5
done
```

**Expected**: First 10-20 requests succeed (or fail with 401), subsequent requests return 429

---

### 6. Security Validation ✓

#### Verify Protected Endpoints Require Auth
```bash
# Should return 401 Unauthorized (not 404)
curl -X GET http://localhost:8080/api/v1/users/me

# Should return 401 Unauthorized (not 404)
curl -X GET http://localhost:8080/api/v1/users

# Should return 401 Unauthorized (not 404)
curl -X GET http://localhost:8080/api/v1/organizations
```

**Expected**: All return 401 Unauthorized, not 404 Not Found

#### Verify Public Endpoints Don't Require Auth
```bash
# Should NOT return 401 (may return 400 or 404 depending on data)
curl -X POST http://localhost:8080/api/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"public@test.com"}'

# Should NOT return 401
curl -X POST http://localhost:8080/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d '{"token":"test"}'
```

**Expected**: No 401 responses for public endpoints

#### Verify Role Enforcement
```bash
# Regular user token should get 403 Forbidden (not 404)
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer REGULAR_USER_JWT_TOKEN"

# Admin token should get 403 Forbidden for super admin endpoints (not 404)
curl -X GET http://localhost:8080/api/v1/admin/tenants \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

**Expected**: 403 Forbidden for insufficient permissions, not 404 Not Found

---

### 7. Error Response Validation ✓

#### Verify Consistent Error Format
```bash
# Should return structured error response
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"nonexistent","password":"wrong"}'
```

**Expected Response Structure**:
```json
{
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "...",
    "timestamp": "...",
    "path": "/api/v1/auth/login",
    "method": "POST",
    "correlationId": "...",
    "requestId": "..."
  }
}
```

---

## Monitoring Checklist

### Gateway Logs
- [ ] No 404 errors for auth-service endpoints
- [ ] Proper routing to auth-service for all paths
- [ ] Rate limiting triggers correctly
- [ ] JWT validation working correctly

### Auth Service Logs
- [ ] Receiving requests from gateway
- [ ] User context propagation working
- [ ] Tenant context propagation working
- [ ] No unexpected errors

### Redis
- [ ] Rate limiting keys being created
- [ ] Keys expiring correctly
- [ ] No connection errors

---

## Rollback Criteria

Rollback if:
- [ ] Any critical endpoint returns 404 (should be routed)
- [ ] Public endpoints require authentication incorrectly
- [ ] Protected endpoints don't require authentication
- [ ] Rate limiting not working
- [ ] Gateway service fails to start
- [ ] Significant increase in error rates

---

## Success Criteria

✅ All 7 endpoint groups accessible through gateway:
1. Authentication endpoints
2. Email verification endpoints
3. Password management endpoints
4. User profile endpoints
5. User management endpoints
6. Organization management endpoints
7. Tenant management endpoints

✅ Security properly configured:
- Public endpoints accessible without auth
- Protected endpoints require JWT
- Admin endpoints enforce role requirements
- Rate limiting active on all endpoints

✅ No breaking changes:
- Existing functionality still works
- No new errors in logs
- Performance not degraded

---

## Sign-off

- [ ] All public endpoints tested and working
- [ ] All authenticated endpoints tested and working
- [ ] All admin endpoints tested and working
- [ ] Rate limiting validated
- [ ] Security validation passed
- [ ] Error responses consistent
- [ ] Logs reviewed - no issues
- [ ] Ready for production deployment

**Tested by**: _________________  
**Date**: _________________  
**Environment**: _________________  
**Notes**: _________________
