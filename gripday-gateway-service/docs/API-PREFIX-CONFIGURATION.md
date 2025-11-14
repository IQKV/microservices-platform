# API Prefix Configuration

## Overview

The gateway supports configurable API prefixes that can be adjusted per environment. This allows you to use `/api/*` paths in development/staging while stripping the prefix when deployed on a dedicated API subdomain like `api.gripday.com`.

## Configuration Properties

```yaml
gripday:
  gateway:
    routing:
      api-prefix:
        enabled: true # Enable/disable prefix handling
        prefix: /api # The prefix to use (can be empty)
        strip-count: 0 # Number of path segments to strip (0 = no stripping)
```

## Environment-Specific Behavior

### Local Development (`application-local.yml`)

- **Prefix**: `/api`
- **Strip Count**: `0` (no stripping)
- **URLs**: `http://localhost:8080/api/v1/auth/login`

### Staging (`application-staging.yml`)

- **Prefix**: `/api`
- **Strip Count**: `0` (no stripping)
- **URLs**: `https://staging.gripday.com/api/v1/auth/login`

### Production (`application-production.yml`)

- **Prefix**: `` (empty - no prefix)
- **Strip Count**: `0` (no stripping needed)
- **URLs**: `https://api.gripday.com/v1/auth/login`

## How It Works

### Route Configuration

Routes are configured with the prefix dynamically injected:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8080
          predicates:
            - Path=${gripday.gateway.routing.api-prefix.prefix}${gripday.gateway.routing.services.user-service.path}
          filters:
            - StripPrefix=${gripday.gateway.routing.api-prefix.strip-count:0}
```

### Service Path Configuration

Service paths are defined without the `/api` prefix:

```yaml
gripday:
  gateway:
    routing:
      services:
        user-service:
          path: /v1/auth/** # No /api prefix in path definition
```

### Combined Result

- **Local/Staging**: `/api` + `/v1/auth/**` = `/api/v1/auth/**`
- **Production**: ``+`/v1/auth/**`=`/v1/auth/**`

## Public Paths Configuration

Public paths must include both variants to support all environments:

```yaml
gripday:
  gateway:
    security:
      public-paths:
        # With /api prefix (local/staging)
        - /api/v1/auth/login
        - /api/v1/auth/signup

        # Without /api prefix (production)
        - /v1/auth/login
        - /v1/auth/signup
```

## Rate Limiting Configuration

Rate limiting endpoints should use the full path including prefix where applicable:

```yaml
gripday:
  gateway:
    rate-limiting:
      policies:
        endpoints:
          "/api/v1/auth/login": # For local/staging
            requests-per-minute: 10
          "/v1/auth/login": # For production
            requests-per-minute: 10
```

## Deployment Scenarios

### Scenario 1: Development (localhost)

- Gateway: `http://localhost:8080`
- Frontend calls: `http://localhost:8080/api/v1/auth/login`
- Backend receives: `/v1/auth/login` (if strip-count=1) or `/api/v1/auth/login` (if strip-count=0)

### Scenario 2: Production (api.gripday.com)

- Gateway: `https://api.gripday.com`
- Frontend calls: `https://api.gripday.com/v1/auth/login`
- Backend receives: `/v1/auth/login`
- No `/api` prefix needed since domain already indicates API

## Migration Guide

### Updating Existing Routes

1. **Update service path definitions** - Remove `/api` prefix:

   ```yaml
   # Before
   path: /api/v1/service/**

   # After
   path: /v1/service/**
   ```

2. **Add prefix configuration** per environment:

   ```yaml
   # Local/Staging
   api-prefix:
     prefix: /api
     strip-count: 0

   # Production
   api-prefix:
     prefix: ""
     strip-count: 0
   ```

3. **Update route predicates** to use dynamic prefix:

   ```yaml
   # Before
   predicates:
     - Path=/api/v1/service/**

   # After
   predicates:
     - Path=${gripday.gateway.routing.api-prefix.prefix}${gripday.gateway.routing.services.service-name.path}
   ```

4. **Update public paths** to include both variants

5. **Update rate limiting** endpoint configurations

## Best Practices

1. **Use empty prefix in production** when deployed on `api.gripday.com`
2. **Keep `/api` prefix in development** for clear API identification
3. **Always include both path variants** in public paths configuration
4. **Test thoroughly** in staging before production deployment
5. **Document frontend API base URLs** per environment
