# ADR 0001: Use Schema-Per-Tenant Multi-Tenancy Strategy

## Status

Accepted

## Context

The billing service needs to support multiple tenants (customers) with strict data isolation requirements. We need to choose a multi-tenancy strategy that balances:

- **Data Isolation**: Ensure tenant data cannot be accessed by other tenants
- **Performance**: Minimize query overhead and maintain good performance
- **Scalability**: Support growing number of tenants
- **Compliance**: Meet regulatory requirements (GDPR, SOC 2)
- **Operational Complexity**: Manage database migrations and backups

Three main approaches were considered:

1. **Shared Schema**: All tenants share the same tables with a tenant_id discriminator column
2. **Schema-Per-Tenant**: Each tenant gets a dedicated database schema
3. **Database-Per-Tenant**: Each tenant gets a dedicated database instance

## Decision

We will use **Schema-Per-Tenant** multi-tenancy strategy with Hibernate's multi-tenancy support.

## Rationale

### Advantages of Schema-Per-Tenant

1. **Strong Data Isolation**
   - Physical separation at schema level
   - Impossible to accidentally query another tenant's data
   - Easier to audit and demonstrate compliance

2. **Performance Benefits**
   - No tenant_id filtering on every query
   - Better query plan optimization
   - Smaller indexes per schema

3. **Tenant-Specific Operations**
   - Easy to backup/restore individual tenant data
   - Can apply tenant-specific migrations
   - Simpler data export for GDPR compliance

4. **Scalability**
   - Can move schemas to different database instances if needed
   - Better than database-per-tenant (less overhead)
   - More scalable than shared schema (no large table issues)

### Why Not Shared Schema?

- Risk of data leakage if tenant_id filter is missed
- Large tables with millions of rows across all tenants
- Complex queries with tenant_id in every WHERE clause
- Difficult to isolate tenant data for compliance

### Why Not Database-Per-Tenant?

- High operational overhead (managing hundreds of databases)
- Connection pool exhaustion
- Expensive for small tenants
- Complex backup and monitoring

## Implementation

### Hibernate Configuration

```java
@Configuration
public class MultiTenancyConfig {

  @Bean
  public CurrentTenantIdentifierResolver currentTenantIdentifierResolver() {
    return new CurrentTenantIdentifierResolverImpl();
  }

  @Bean
  public MultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
    return new SchemaPerTenantConnectionProvider(dataSource);
  }
}
```

### Tenant Context Management

```java
public final class TenantContext {

  private static final ThreadLocal<String> currentTenantId = new ThreadLocal<>();

  public static void setCurrentTenantId(String tenantId) {
    currentTenantId.set(tenantId);
  }

  public static String getCurrentTenantId() {
    return currentTenantId.get();
  }

  public static void clear() {
    currentTenantId.remove();
  }
}
```

### Schema Naming Convention

- **Public Schema**: `public` - Stores subscription plans (shared across tenants)
- **Tenant Schemas**: `tenant_{uuid}` - Stores tenant-specific billing data

### Migration Strategy

- **System Migrations**: Applied to public schema (subscription plans)
- **Tenant Migrations**: Applied to each tenant schema (subscriptions, invoices, payments)

## Consequences

### Positive

- Strong data isolation and security
- Better performance for tenant-specific queries
- Easier compliance and auditing
- Simpler data export/deletion for GDPR
- Can scale by moving schemas to different databases

### Negative

- More complex database migrations (must run per tenant)
- Slightly higher operational overhead than shared schema
- Need to manage schema creation for new tenants
- Connection pool must support multiple schemas

### Mitigation Strategies

1. **Automated Schema Creation**: Create tenant schema automatically on first subscription
2. **Migration Tooling**: Use Liquibase to apply migrations to all tenant schemas
3. **Monitoring**: Track schema count and size per tenant
4. **Connection Pooling**: Configure HikariCP with appropriate pool size

## Alternatives Considered

### Alternative 1: Shared Schema with Row-Level Security

**Pros**:

- Simpler migrations
- Lower operational overhead

**Cons**:

- Risk of data leakage
- Performance overhead on every query
- Complex RLS policies

**Rejected**: Security risk too high for billing data

### Alternative 2: Database-Per-Tenant

**Pros**:

- Maximum isolation
- Easy to scale individual tenants

**Cons**:

- Very high operational overhead
- Connection pool exhaustion
- Expensive for small tenants

**Rejected**: Operational complexity too high

## References

- [Hibernate Multi-Tenancy Documentation](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#multitenacy)
- [Multi-Tenancy Patterns](https://docs.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models)
- [PostgreSQL Schema Documentation](https://www.postgresql.org/docs/current/ddl-schemas.html)

## Date

2024-12-09

## Author

Billing Service Team
