# Demo Contact Records

This directory contains demo contact data migrations for development and testing purposes. The migrations create a complete CRM scenario for the `demo-tenant` organization with companies, contacts, activities, and webhook integrations.

## Demo CRM Scenario

### Organization Context
- **Tenant**: `demo-tenant` (Demo Tech Solutions)
- **Schema**: `demo_tenant` (isolated tenant schema)
- **Industry**: Technology/SaaS Platform Provider
- **Business Model**: B2B Enterprise Software Sales

## Demo Records Created

### 1. TechCorp Industries (Prospect Company)
- **Company ID**: Auto-generated
- **Status**: PROSPECT
- **Industry**: Software Development
- **Size**: 201-500 employees
- **Location**: Austin, Texas
- **Website**: https://techcorp-industries.com

**Business Context:**
- Mid-size software company evaluating CRM modernization
- Initial contact made at Tech Conference 2024
- Strong interest in enterprise SaaS platform
- Decision timeline: Q1 2025
- Budget approved for $50K+ solution

**Key Contact - Sarah Chen (CTO):**
- **Email**: sarah.chen@techcorp-industries.com
- **Status**: LEAD
- **Lead Score**: 85 (High-value prospect)
- **Role**: Chief Technology Officer
- **Engagement**: Attended product demo, downloaded whitepaper, requested POC
- **Decision Authority**: Key decision maker for technology purchases

### 2. Global Retail Solutions (Active Customer)
- **Company ID**: Auto-generated
- **Status**: ACTIVE (Converted Customer)
- **Industry**: Retail Technology
- **Size**: 1001-5000 employees
- **Location**: Chicago, Illinois
- **Website**: https://globalretail.com

**Business Context:**
- Large retail technology company
- Successfully converted from prospect in November 2024
- Currently using Enterprise plan ($180K annual contract value)
- Excellent relationship with IT team
- Potential for expansion to additional business units

**Key Contact - Michael Rodriguez (VP IT):**
- **Email**: m.rodriguez@globalretail.com
- **Status**: CUSTOMER (Converted)
- **Lead Score**: 95 (Highest value)
- **Role**: Vice President of Information Technology
- **Conversion**: Converted from lead ID 12345 on November 15, 2024
- **Relationship**: Excellent, potential advocate for expansion

### 3. StartupHub Accelerator (Strategic Partner)
- **Company ID**: Auto-generated
- **Status**: PARTNER
- **Industry**: Venture Capital
- **Size**: 11-50 employees
- **Location**: San Francisco, California
- **Website**: https://startuphub.io

**Business Context:**
- Strategic partner accelerator for startup referrals
- Partnership agreement signed October 2024
- Provides 20% discount codes to portfolio companies
- Quarterly business reviews scheduled
- Exploring co-marketing opportunities

**Key Contact - Jennifer Park (Partnership Director):**
- **Email**: jennifer.park@startuphub.io
- **Status**: ACTIVE (Partner Contact)
- **Lead Score**: 70 (Strategic value)
- **Role**: Director of Strategic Partnerships
- **Value**: Manages referral program, sends qualified leads

## Demo Activities Created

### 1. Technical Requirements Discussion (Pending)
- **Contact**: Sarah Chen (TechCorp Industries)
- **Type**: CALL
- **Status**: PENDING
- **Priority**: HIGH
- **Due Date**: January 25, 2025 at 2:00 PM UTC
- **Assigned To**: admin (Bob Smith)

**Purpose:**
Follow-up call to discuss technical requirements for proof of concept including:
- API integration capabilities
- Data migration timeline
- Security compliance requirements
- POC success criteria

### 2. Q4 2024 Quarterly Business Review (Completed)
- **Contact**: Michael Rodriguez (Global Retail Solutions)
- **Type**: MEETING
- **Status**: COMPLETED
- **Priority**: MEDIUM
- **Completed**: December 20, 2024 at 11:30 AM UTC
- **Assigned To**: billing (Carol Williams)

**Outcomes:**
- Platform usage metrics: 95% user adoption
- ROI analysis: $2.3M in efficiency gains
- Upcoming feature requests documented
- Expansion to European operations discussed for Q2 2025

### 3. 2025 Partnership Strategy Planning (Pending)
- **Contact**: Jennifer Park (StartupHub Accelerator)
- **Type**: MEETING
- **Status**: PENDING
- **Priority**: MEDIUM
- **Due Date**: January 30, 2025 at 3:00 PM UTC
- **Assigned To**: owner (Alice Johnson)

**Agenda:**
- Referral program optimization
- Co-marketing campaign planning
- Joint webinar series development
- Startup success metrics tracking
- Expanded partner benefits program
- Goal: 50% increase in qualified referrals

## Demo Webhook Integration

### External CRM Sync Webhook
- **Name**: External CRM Sync
- **URL**: https://external-crm.demo.com/webhooks/contact-events
- **Status**: ACTIVE
- **Events**: contact.created, contact.updated, contact.deleted, contact.converted
- **Security**: Webhook secret authentication
- **Performance**: 47 successful deliveries, 2 failures
- **Last Triggered**: December 20, 2024

**Purpose:**
Demonstrates real-time integration capabilities for syncing contact events with external CRM systems, maintaining data consistency across platforms.

## Lead Scoring & Conversion Tracking

### Lead Score Distribution
- **Sarah Chen**: 85 (High-value lead, actively engaged)
- **Michael Rodriguez**: 95 (Converted customer, highest value)
- **Jennifer Park**: 70 (Strategic partner value)

### Conversion Tracking
- **Michael Rodriguez**: Successfully converted from lead ID 12345
- **Conversion Date**: November 15, 2024 at 4:30 PM UTC
- **Conversion Path**: Lead → Prospect → Customer
- **Timeline**: 3-month sales cycle from initial contact to conversion

## Data Relationships

### Company Hierarchy
```
TechCorp Industries (PROSPECT)
├── Sarah Chen (LEAD, Score: 85)
└── Technical Requirements Call (PENDING)

Global Retail Solutions (ACTIVE)
├── Michael Rodriguez (CUSTOMER, Score: 95)
└── Quarterly Business Review (COMPLETED)

StartupHub Accelerator (PARTNER)
├── Jennifer Park (ACTIVE, Score: 70)
└── Partnership Strategy Meeting (PENDING)
```

### Contact Status Flow
```
LEAD (Sarah Chen) → Evaluation → POC → Decision
CUSTOMER (Michael Rodriguez) → Onboarded → Active → Expansion
ACTIVE (Jennifer Park) → Partnership → Referrals → Growth
```

## Business Scenarios Demonstrated

### 1. Lead Management & Conversion
- **Lead Qualification**: High-scoring leads with engagement tracking
- **Lead Nurturing**: Scheduled follow-up activities and touchpoints
- **Conversion Tracking**: Complete audit trail from lead to customer
- **Lead Scoring**: Behavioral and demographic scoring models

### 2. Customer Success Management
- **Onboarding**: Successful customer conversion and activation
- **Relationship Management**: Regular business reviews and check-ins
- **Expansion Opportunities**: Identifying upsell and cross-sell potential
- **Customer Advocacy**: Leveraging satisfied customers for references

### 3. Partnership Development
- **Strategic Partnerships**: Referral programs and co-marketing initiatives
- **Partner Management**: Regular strategy sessions and performance reviews
- **Lead Generation**: Partner-driven lead acquisition channels
- **Mutual Value Creation**: Win-win partnership structures

### 4. Sales Pipeline Management
- **Pipeline Stages**: Lead → Prospect → Customer progression
- **Activity Tracking**: Calls, meetings, and follow-up activities
- **Priority Management**: High, medium, low priority classification
- **Assignment Management**: Activity ownership and accountability

## Testing Scenarios

### Contact Management Testing
- Test contact CRUD operations with various statuses
- Verify lead scoring calculations and updates
- Test contact search and filtering capabilities
- Validate email uniqueness constraints

### Company Management Testing
- Test company hierarchy relationships (parent/child)
- Verify company status transitions
- Test company-contact associations
- Validate address and contact information

### Activity Management Testing
- Test activity creation and assignment
- Verify due date tracking and overdue detection
- Test activity completion workflows
- Validate activity-contact-company relationships

### Lead Conversion Testing
- Test lead-to-customer conversion workflows
- Verify conversion timestamp tracking
- Test lead scoring updates and thresholds
- Validate conversion audit trails

### Webhook Integration Testing
- Test webhook event publishing for contact lifecycle
- Verify webhook retry mechanisms and failure handling
- Test webhook security and authentication
- Validate event payload structure and content

## Development Usage

### Running Demo Migrations
```bash
# Run with demo context to include demo data
mvn liquibase:update -Dliquibase.contexts=demo

# Run system migrations only (no demo data)
mvn liquibase:update -Dliquibase.contexts=system

# Run tenant migrations only (no demo data)
mvn liquibase:update -Dliquibase.contexts=tenant
```

### Accessing Demo Data
Use the demo contact records to test:
- Contact management APIs and bulk operations
- Lead scoring and conversion tracking
- Activity management and assignment workflows
- Company hierarchy and relationship management
- Webhook event publishing and integration

### Integration with Other Services
The demo contact data is designed to work with:
- **User Service**: Activities assigned to demo users (owner, admin, billing, manager)
- **Billing Service**: Customer contacts linked to subscription management
- **Lead Service**: Lead conversion tracking and pipeline management

## API Testing Examples

### Contact Operations
```bash
# Get high-scoring leads
GET /api/v1/contacts?status=LEAD&minLeadScore=80

# Search for TechCorp contacts
GET /api/v1/contacts?search=techcorp

# Get contacts by company
GET /api/v1/contacts/company/{techcorp-company-id}

# Update lead score
PATCH /api/v1/contacts/{sarah-chen-id}/score
```

### Activity Operations
```bash
# Get pending activities
GET /api/v1/activities?status=PENDING

# Get activities assigned to admin
GET /api/v1/activities?assignedTo=admin

# Get overdue activities
GET /api/v1/activities?overdue=true
```

### Company Operations
```bash
# Get prospect companies
GET /api/v1/companies?status=PROSPECT

# Search companies by industry
GET /api/v1/companies?industry=Software Development

# Get company hierarchy
GET /api/v1/companies/{company-id}/children
```

## Rollback Support

All demo migrations include comprehensive rollback scripts that remove data in dependency order:
1. Webhook configurations
2. Activities (contact and company references)
3. Contacts (company references)
4. Companies
5. Tenant info

```bash
# Rollback demo migrations
mvn liquibase:rollback -Dliquibase.rollbackCount=2
```