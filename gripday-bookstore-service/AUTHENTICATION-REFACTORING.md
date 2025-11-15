# Authentication Refactoring - Bookstore Service

## Summary

Refactored authentication implementation to use Spring Security's declarative approach with `@PreAuthorize` annotations, eliminating redundant service-layer authorization checks.

## Changes Made

### 1. SecurityConfiguration.java

**Changed:** Role-based to Authority-based authorization

- Replaced `.hasAnyRole("ADMIN", "SUPERADMIN")` with `.hasAnyAuthority("ADMIN", "SUPERADMIN")`
- Added custom `JwtAuthenticationConverter` with `jwtGrantedAuthoritiesConverter()`
- Extracts authorities from multiple JWT claim sources:
  - `roles` claim
  - `authorities` claim
  - `realm_access.roles` (Keycloak compatibility)
- Authorities are used directly without "ROLE\_" prefix

### 2. BookService.java

**Removed:** Manual authorization checks in service methods

- Removed `if (!userContext.isAdmin())` checks
- Removed `auditLogger.logUnauthorizedAccess()` calls
- Removed `bookstoreMetrics.incrementUnauthorizedAccess()` calls
- Removed `UnauthorizedOperationException` throws

**Added:** Method-level security annotations

- `@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")` on:
  - `createBook()`
  - `updateBook()`
  - `deleteBook()`

### 3. InventoryService.java

**Removed:** Manual authorization checks in service methods

- Removed `if (!userContext.isAdmin())` checks from:
  - `updateInventory()`
  - `bulkUpdateInventory()`
  - `adjustInventoryQuantity()`
- Removed associated audit logging and metrics calls

**Added:** Method-level security annotations

- `@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")` on:
  - `updateInventory()`
  - `bulkUpdateInventory()`
  - `adjustInventoryQuantity()`

### 4. Removed Imports

**BookService.java:**

- `org.gripday.bookstore.domain.exception.UnauthorizedOperationException`

**InventoryService.java:**

- `org.gripday.bookstore.domain.exception.UnauthorizedOperationException`

**Added Imports:**

- `org.springframework.security.access.prepost.PreAuthorize` (both services)
- `org.springframework.core.convert.converter.Converter` (SecurityConfiguration)
- `org.springframework.security.core.GrantedAuthority` (SecurityConfiguration)
- `org.springframework.security.core.authority.SimpleGrantedAuthority` (SecurityConfiguration)
- `org.springframework.security.oauth2.jwt.Jwt` (SecurityConfiguration)
- `org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter` (SecurityConfiguration)

## Benefits

1. **Cleaner Code:** Removed ~40 lines of redundant authorization logic
2. **Single Responsibility:** Services focus on business logic, not authorization
3. **Consistency:** All authorization handled uniformly by Spring Security
4. **Maintainability:** Authorization rules in one place (SecurityConfiguration + @PreAuthorize)
5. **Correctness:** Fixed role prefix issue (ROLE\_ vs no prefix)
6. **Flexibility:** Easy to add more complex authorization expressions

## Authorization Flow

### Before

1. HTTP request → SecurityConfiguration (role check)
2. Controller → Service
3. Service → Manual `userContext.isAdmin()` check
4. If unauthorized → throw exception, log audit, increment metrics

### After

1. HTTP request → SecurityConfiguration (authority check)
2. JWT → JwtAuthenticationConverter → Extract authorities
3. Controller → Service
4. @PreAuthorize → Spring Security checks authorities
5. If unauthorized → Spring Security throws AccessDeniedException (403)

## Testing Considerations

- Spring Security's `@PreAuthorize` is automatically tested by integration tests
- No need to test authorization logic in service unit tests
- Focus service tests on business logic only
- Use `@WithMockUser(authorities = {"ADMIN"})` in tests for admin operations

## Backward Compatibility

- API endpoints remain unchanged
- HTTP status codes remain the same (403 for unauthorized)
- JWT token format unchanged
- UserContext propagation unchanged
- Audit logging for successful operations unchanged

## Notes

- `UnauthorizedOperationException` is no longer used and can be removed if not referenced elsewhere
- `AuditLogger.logUnauthorizedAccess()` is no longer called from services
- `BookstoreMetrics.incrementUnauthorizedAccess()` is no longer called from services
- Spring Security will handle 403 responses automatically via `AccessDeniedException`
