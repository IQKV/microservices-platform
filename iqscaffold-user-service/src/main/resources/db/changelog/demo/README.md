# Demo Users and Authorities

This directory contains demo data migrations for development and testing purposes. The migrations create a complete set of users with different authority levels to demonstrate the multi-tenant RBAC system.

## Demo Tenant

- **Tenant ID**: `demo-tenant`
- **Organization**: Demo Tech Solutions
- **Industry**: Technology
- **Subscription**: Enterprise plan with 100 user limit

## Demo Users

### 1. Super Administrator (`superadmin`)
- **Email**: superadmin@iqscaffold.com
- **Password**: SuperAdmin123!
- **Authority**: SUPER_ADMIN
- **Description**: Platform administrator with full system access across all tenants. Responsible for system-wide operations, security, and multi-tenant infrastructure management.
- **Features**: 
  - Cross-tenant access
  - System configuration
  - Platform monitoring
  - Security oversight
- **2FA**: Enabled (TOTP)
- **Theme**: Dark mode for extended monitoring sessions

### 2. Tenant Owner (`owner`)
- **Email**: owner@demo.iqscaffold.com
- **Password**: TenantOwner123!
- **Authority**: TENANT_OWNER
- **Description**: Organization owner and CEO responsible for strategic decisions, billing oversight, and business growth initiatives. Cannot be removed, only transferred.
- **Features**:
  - Full billing and subscription management
  - Organization settings control
  - User management within tenant
  - Financial reporting access
- **2FA**: Enabled (SMS)
- **Timezone**: America/New_York (EST)

### 3. Tenant Administrator (`admin`)
- **Email**: admin@demo.iqscaffold.com
- **Password**: TenantAdmin123!
- **Authority**: TENANT_ADMIN
- **Description**: Technical administrator managing user accounts, system configuration, and day-to-day operations within the tenant.
- **Features**:
  - Full tenant administration
  - User account management
  - System configuration
  - Operational oversight
- **2FA**: Enabled (TOTP)
- **Theme**: Dark mode for technical work

### 4. Billing Administrator (`billing`)
- **Email**: billing@demo.iqscaffold.com
- **Password**: BillingAdmin123!
- **Authority**: BILLING_ADMIN
- **Description**: Billing administrator responsible for subscription management, invoice processing, payment gateway integration, and financial reporting.
- **Features**:
  - Subscription management
  - Invoice processing
  - Payment gateway configuration
  - Financial reporting
  - Billing notifications
- **2FA**: Enabled (Email)
- **Timezone**: America/Chicago (CST)

### 5. Finance Viewer (`finance`)
- **Email**: finance@demo.iqscaffold.com
- **Password**: FinanceViewer123!
- **Authority**: FINANCE_VIEWER
- **Description**: Financial analyst with read-only access to billing data, reports, and analytics for business intelligence and compliance purposes.
- **Features**:
  - Read-only financial data access
  - Report generation
  - Analytics dashboard
  - Compliance reporting
- **2FA**: Disabled (read-only access)
- **Access Level**: View-only

### 6. Manager (`manager`)
- **Email**: manager@demo.iqscaffold.com
- **Password**: RegularAdmin123!
- **Authority**: ADMIN
- **Description**: Department manager responsible for team coordination, user management, and operational oversight without billing access.
- **Features**:
  - User management (non-billing)
  - Team coordination
  - Operational reports
  - Configuration management
- **2FA**: Disabled
- **Timezone**: America/Denver (MST)

### 7. Standard User (`user`)
- **Email**: user@demo.iqscaffold.com
- **Password**: StandardUser123!
- **Authority**: USER
- **Description**: Standard application user with basic access to core features and functionality for daily business operations.
- **Features**:
  - Core application access
  - Basic functionality
  - Personal preferences
  - Standard reporting
- **2FA**: Disabled
- **Access Level**: Basic user

## Authority Hierarchy

```
SUPER_ADMIN (Platform-wide)
├── TENANT_OWNER (Organization owner)
├── TENANT_ADMIN (Full tenant control)
├── BILLING_ADMIN (Financial management)
├── FINANCE_VIEWER (Read-only financial)
├── ADMIN (User management, no billing)
└── USER (Basic access)
```

## Usage

### Running Demo Migrations

The demo migrations are included with context `demo` and will run automatically when the application starts in development mode. To run only demo migrations:

```bash
# Run with demo context
mvn liquibase:update -Dliquibase.contexts=demo
```

### Accessing Demo Users

All demo users have verified emails and are enabled by default. Use the credentials above to test different authority levels and access patterns.

### Security Notes

- All passwords are BCrypt hashed with strength 12
- Demo users should only be used in development/testing environments
- Production deployments should exclude demo context migrations
- 2FA settings demonstrate different authentication methods (TOTP, SMS, Email)

## Development Testing Scenarios

### Multi-Tenant Testing
- Use `superadmin` to test cross-tenant operations
- Use `owner` to test tenant-specific administrative functions

### RBAC Testing
- Test authority-based access control with different user roles
- Verify permission boundaries between authorities

### Billing Integration Testing
- Use `billing` user to test payment gateway integration
- Use `finance` user to test read-only financial access

### User Management Testing
- Use `admin` or `manager` to test user management workflows
- Test user creation, modification, and deactivation

## Rollback

The migration includes a comprehensive rollback that removes all demo data in reverse order:
1. User preferences
2. User authorities
3. Users
4. Organization
5. Tenant

```bash
# Rollback demo migrations
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```