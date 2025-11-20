# Step-by-Step: Refactor Business Logic to Schema-per-Tenant (Liquibase XML) — No Data Migration

Purpose: Refactor application logic to use schema-per-tenant with Hibernate SCHEMA multi-tenancy and Liquibase XML changesets, without moving existing data now.


## Prerequisites
- Postgres (recommended) or another DB supporting schemas
- Existing tenant resolution: `TenantExtractionService` + `TenantContext`
- Java/Spring Boot app with JPA/Hibernate
- Liquibase dependencies on classpath


## 1) Set configuration baselines
- Keep entity DDL management off in runtime:
  - `spring.jpa.hibernate.ddl-auto=validate`
- Enable Hibernate SCHEMA multi-tenancy:
  - `spring.jpa.properties.hibernate.multiTenancy=SCHEMA`
  - `spring.jpa.properties.hibernate.tenant_identifier_resolver=org.gripday.userservice.tenancy.SchemaTenantIdentifierResolver`
- Disable global Liquibase auto-run (we will run per schema programmatically):
  - `spring.liquibase.enabled=false`
- App props (example):
  - `app.tenancy.schema.prefix=tenant_`
  - `app.liquibase.systemChangeLog=classpath:db/changelog/system/master.xml`
  - `app.liquibase.tenantChangeLog=classpath:db/changelog/tenant/master.xml`


## 2) Add SchemaNameResolver
- Responsibility: map `tenantId -> schemaName` with normalization and caching.
- Normalization rules (suggested): lowercase, replace `-` with `_`, drop illegal chars, length-safe.
- Provide a method `toSchema(tenantId)` and optionally `exists(schema)`.


## 3) Implement SchemaTenantIdentifierResolver
- Implements Hibernate `CurrentTenantIdentifierResolver`.
- Reads `tenantId` from `TenantContext`, converts to `schema` via `SchemaNameResolver`.
- Falls back to `public` (system endpoints) if no tenant context.


## 4) Organize Liquibase changelogs
- Create folders:
  - `db/changelog/system/master.xml` (global/public objects only)
  - `db/changelog/tenant/master.xml` (tenant-scoped objects)
- Move existing Liquibase XML changesets (e.g., `003-create-user-authorities-table.xml`) under tenant master via `<include>`.
- Keep changesets idempotent with `<preConditions>` and `IF NOT EXISTS` where applicable.


## 5) Create TenantLiquibaseRunner (programmatic)
- Service that executes Liquibase against a target schema:
  - Set `defaultSchema` and `liquibaseSchema` to the tenant schema so `DATABASECHANGELOG` tables live per tenant.
  - Use `tenant/master.xml` changelog.
- Create a similar one-time runner for `system/master.xml` against `public` at startup.


## 6) Provisioning flow (logic only, no data move)
- On tenant creation:
  1. Compute schema: `schema = SchemaNameResolver.toSchema(tenantId)`
  2. `CREATE SCHEMA IF NOT EXISTS <schema>`
  3. Grant privileges to app role
  4. Run `TenantLiquibaseRunner` for `<schema>` using `tenant/master.xml`
  5. Mark tenant enabled
- Decommissioning (future): disable → backup → drop schema → revoke


## 7) Refactor service layer
- Remove ad-hoc `tenant_id` checks in business logic for tenant-scoped entities.
- Validate tenant access at boundaries (controllers/security) using `TenantContext` and authorization.
- Keep `tenant_id` in entities only if still needed for compatibility; avoid using it for isolation decisions.


## 8) Refactor repository layer
- Eliminate manual `tenant_id` predicates for tenant data.
- Rely on Hibernate schema routing (via `SchemaTenantIdentifierResolver`).
- Verify all custom queries work without `tenant_id` filters.


## 9) Caching and events
- Ensure cache keys are tenant/schema-aware (use `TenantContext` or prefix keys with schema).
- Include tenant/schema identifiers in emitted events and message headers.


## 10) Auditing and observability
- Add `schema` (or tenant) to MDC and audit records.
- Verify tracing correlates requests to the active tenant/schema.


## 11) Authorization boundary
- SUPER_ADMIN can switch across schemas (explicit context).
- Tenant users restricted to current tenant context; enforce in security layer (e.g., method-level security/policies).


## 12) Testing
- Unit: Schema name normalization, resolver behavior (null/missing tenant → `public`).
- Integration:
  - Start a Postgres test container
  - Create two schemas (t1, t2), run tenant changelog for both
  - With `TenantContext=t1`, writes are invisible from `t2` and vice versa
- E2E: Tenant provisioning creates schema and applies changelog successfully.


## 13) Rollout
- Flagless rollout (support SCHEMA model from the start):
  - Ensure configuration is set for SCHEMA multi-tenancy across all services.
  - Run `system/master.xml` once at startup against `public`.
  - Pre-create required tenant schemas (or create on first tenant onboarding) and apply `tenant/master.xml`.
  - Validate isolation with integration smoke tests before accepting traffic.
  - Deploy normally and monitor logs/metrics for schema routing and Liquibase outcomes.


## 14) Operational runbook
- New tenant: create schema → run Liquibase per schema → enable tenant.
- Release: iterate tenants and run Liquibase per schema; verify status; proceed.
- Decommission: disable → backup → drop schema → revoke privileges.


## Notes
- This plan does not move or transform existing tenant data. It prepares the runtime and business logic for schema-per-tenant now and enables safe data migration later if desired.
