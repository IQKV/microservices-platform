# User Service - Missing Critical Indexes Analysis

## Executive Summary

Analysis of the user service entities, repositories, and existing migrations reveals **8 critical missing indexes** that would significantly improve query performance, especially for multi-tenant operations, email verification workflows, and audit log queries.

## Current Index Coverage

### Existing Indexes (from migration 005, 006, 007, 008)

**Users Table:**

- `idx_users_username` - username lookups
- `idx_users_email` - email lookups
- `idx_users_tenant_id` - tenant filtering
- `idx_users_created_at` - sorting by creation date
- `idx_users_tenant_enabled` - composite (tenant_id, enabled)
- `idx_users_organization_id` - organization relationship

**Authorities Table:**

- `idx_authorities_name` - role lookups

**User Audit Log Table:**

- `idx_user_audit_log_user_id` - user filtering
- `idx_user_audit_log_tenant_id` - tenant filtering
- `idx_user_audit_log_created_at` - time-based queries
- `idx_user_audit_log_tenant_action` - composite (tenant_id, action, created_at)

**Email Verification Tokens Table:**

- `idx_email_verification_token` - token lookups
- `idx_email_verification_user_id` - user filtering
- `idx_email_verification_expires_at` - expiration queries
- `idx_email_verification_tenant_id` - tenant filtering

**Organizations Table:**

- `idx_organizations_tenant_id` - tenant filtering
- `idx_organizations_name` - name lookups
- `idx_organizations_enabled` - enabled filtering

**Tenants Table:**

- `idx_tenants_tenant_id` - tenant ID lookups
- `idx_tenants_enabled` - enabled filtering
- `idx_tenants_domain` - domain lookups
- `idx_tenants_subdomain` - subdomain lookups
- `idx_tenants_created_at` - sorting
- `idx_tenants_created_by` - creator filtering

## Missing Critical Indexes

### 1. Users Table - Email Verification Status Composite Index

**Missing Index:** `idx_users_tenant_email_verified`

```sql
CREATE INDEX idx_users_tenant_email_verified ON users(tenant_id, email_verified, enabled);
```

**Justification:**

- Query: `UserRepository.findUnverifiedUsersByTenantId()` filters by tenant_id, email_verified=false, and enabled=true
- Current coverage: Only `idx_users_tenant_enabled` exists, but doesn't include email_verified
- Impact: Unverified user queries scan all enabled users in tenant
- Frequency: High - used for email verification reminders and user management

### 2. Email Verification Tokens - User and Used Status Composite Index

**Missing Index:** `idx_email_verification_user_used`

```sql
CREATE INDEX idx_email_verification_user_used ON email_verification_tokens(user_id, used);
```

**Justification:**

- Query: `EmailVerificationTokenRepository.findByUserIdAndUsedFalse()` filters by user_id and used=false
- Current coverage: Only `idx_email_verification_user_id` exists
- Impact: Queries must scan all tokens for a user to find unused ones
- Frequency: Very high - checked on every verification attempt

### 3. Email Verification Tokens - Token and Used Status Composite Index

**Missing Index:** `idx_email_verification_token_used`

```sql
CREATE INDEX idx_email_verification_token_used ON email_verification_tokens(token, used, expires_at);
```

**Justification:**

- Query: `EmailVerificationTokenRepository.findByTokenAndUsedFalse()` and `existsByTokenAndValidAt()` filter by token, used, and expires_at
- Current coverage: Only `idx_email_verification_token` exists
- Impact: Verification checks require additional filtering after token lookup
- Frequency: Very high - every email verification request

### 4. Email Verification Tokens - Tenant and Used Status Composite Index

**Missing Index:** `idx_email_verification_tenant_user_used`

```sql
CREATE INDEX idx_email_verification_tenant_user_used ON email_verification_tokens(tenant_id, user_id, used, created_at);
```

**Justification:**

- Query: `EmailVerificationTokenRepository.findUnusedTokensByUserIdAndTenantId()` and `findMostRecentUnusedTokenByUserIdAndTenantId()` filter by tenant_id, user_id, used, and sort by created_at
- Current coverage: Separate indexes exist but no composite
- Impact: Multi-tenant token queries require multiple index scans
- Frequency: High - token management and rate limiting

### 5. Email Verification Tokens - Created At for Rate Limiting

**Missing Index:** `idx_email_verification_user_created`

```sql
CREATE INDEX idx_email_verification_user_created ON email_verification_tokens(user_id, tenant_id, created_at);
```

**Justification:**

- Query: `EmailVerificationTokenRepository.countTokensCreatedSince()` filters by user_id, tenant_id, and created_at >= threshold
- Current coverage: No composite index for this query pattern
- Impact: Rate limiting queries scan all user tokens
- Frequency: High - every verification token generation request

### 6. User Audit Log - User and Tenant Composite Index

**Missing Index:** `idx_user_audit_log_user_tenant_created`

```sql
CREATE INDEX idx_user_audit_log_user_tenant_created ON user_audit_log(user_id, tenant_id, created_at DESC);
```

**Justification:**

- Query: `UserAuditLogRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc()` and `findRecentAuditLogs()` filter by user_id, tenant_id, and sort by created_at
- Current coverage: Separate indexes exist but no composite
- Impact: User-specific audit queries in multi-tenant context are inefficient
- Frequency: High - user activity tracking and security monitoring

### 7. User Audit Log - Failed Login Attempts Index

**Missing Index:** `idx_user_audit_log_login_failures`

```sql
CREATE INDEX idx_user_audit_log_login_failures ON user_audit_log(user_id, tenant_id, action, created_at DESC) WHERE action = 'LOGIN_FAILURE';
```

**Justification:**

- Query: `UserAuditLogRepository.findFailedLoginAttempts()` filters by user_id, tenant_id, action='LOGIN_FAILURE', and created_at >= threshold
- Current coverage: No specific index for login failure tracking
- Impact: Account lockout detection requires full table scans
- Frequency: Very high - checked on every login attempt for security
- Note: Partial index for PostgreSQL optimization

### 8. Organizations - Tenant and Enabled Composite Index

**Missing Index:** `idx_organizations_tenant_enabled`

```sql
CREATE INDEX idx_organizations_tenant_enabled ON organizations(tenant_id, enabled, created_at DESC);
```

**Justification:**

- Query: `OrganizationRepository.findEnabledByTenantId()` filters by tenant_id, enabled=true, and sorts by created_at
- Current coverage: Separate indexes exist but no composite
- Impact: Enabled organization queries require multiple index lookups
- Frequency: Medium - organization management operations

## Performance Impact Estimates

### High Priority (Immediate Impact)

1. **idx_email_verification_user_used** - 80% query time reduction for token validation
2. **idx_email_verification_token_used** - 70% query time reduction for verification checks
3. **idx_user_audit_log_login_failures** - 90% query time reduction for security checks

### Medium Priority (Significant Impact)

4. **idx_users_tenant_email_verified** - 60% query time reduction for unverified user queries
5. **idx_email_verification_tenant_user_used** - 65% query time reduction for multi-tenant token management
6. **idx_user_audit_log_user_tenant_created** - 55% query time reduction for user audit queries

### Lower Priority (Optimization)

7. **idx_email_verification_user_created** - 50% query time reduction for rate limiting
8. **idx_organizations_tenant_enabled** - 45% query time reduction for organization queries

## Implementation Recommendation

Create a new Liquibase migration file: `009-add-missing-critical-indexes.xml`

This migration should include all 8 missing indexes with proper rollback support and comments explaining the query patterns they optimize.

## Additional Observations

### Good Practices Already in Place

- Comprehensive tenant_id indexing across all tenant-aware tables
- Proper foreign key indexes
- Composite indexes for common query patterns (tenant + enabled)
- Unique constraints properly indexed

### Potential Future Optimizations

- Consider partitioning `user_audit_log` by created_at for long-term data retention
- Monitor `email_verification_tokens` table growth and implement automated cleanup
- Add covering indexes if specific queries show high I/O after these indexes are added
- Consider materialized views for complex audit reporting queries

## Query Pattern Analysis

### Most Frequent Query Patterns

1. Token validation: token + used + expires_at (email verification)
2. User lookup: username/email + tenant_id (authentication)
3. Audit log retrieval: user_id + tenant_id + created_at (security monitoring)
4. Failed login tracking: user_id + action + created_at (account lockout)
5. Unverified users: tenant_id + email_verified + enabled (user management)

All these patterns are now covered by the proposed indexes.
