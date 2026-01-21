# Demo Lead Records

This directory contains demo lead data migrations for development and testing purposes. The migrations create a complete lead management scenario for the `demo-tenant` organization with leads at different stages of the sales pipeline, including notes, activities, and conversion tracking.

## Demo Lead Management Scenario

### Organization Context
- **Tenant**: `demo-tenant` (Demo Tech Solutions)
- **Schema**: `demo_tenant` (isolated tenant schema)
- **Business Model**: B2B SaaS Platform Provider
- **Sales Process**: Lead Generation → Qualification → Conversion → Customer Success

## Demo Records Created

### 1. David Kim - High-Value Qualified Lead (Enterprise Prospect)
- **Lead ID**: Auto-generated
- **Email**: david.kim@innovatetech.com
- **Company**: InnovateTech Solutions (500+ employees)
- **Job Title**: VP of Engineering
- **Status**: QUALIFIED
- **Lead Score**: 92 (High-value prospect)
- **Source**: Referral (from existing customer Global Retail Solutions)
- **Assigned To**: admin (Bob Smith)

**Business Context:**
- Enterprise prospect with $75K-$100K annual budget
- Strong technical requirements: API integrations, SSO, advanced analytics
- Decision timeline: February 2025
- Referred by existing customer Michael Rodriguez
- Currently in technical evaluation phase

**Lead Journey:**
- Initial referral from satisfied customer
- Discovery call completed with technical requirements gathering
- Qualified based on budget, authority, need, and timeline (BANT)
- Technical demo and POC scheduled
- High conversion probability

### 2. Michael Rodriguez - Successfully Converted Lead (Now Customer)
- **Lead ID**: Auto-generated  
- **Email**: m.rodriguez@globalretail.com
- **Company**: Global Retail Solutions (1001-5000 employees)
- **Job Title**: Vice President of Information Technology
- **Status**: CONVERTED
- **Lead Score**: 95 (Highest value)
- **Source**: Website (contact form inquiry)
- **Conversion Date**: November 15, 2024 at 4:30 PM UTC
- **Contact ID**: 12345 (linked to contact service)
- **Assigned To**: billing (Carol Williams)

**Business Context:**
- Successfully converted to Enterprise customer ($180K annual contract)
- 75-day lead-to-customer conversion cycle
- Demonstrates successful enterprise sales process
- Now serves as reference customer for new prospects
- Led comprehensive CRM evaluation process

**Conversion Success Factors:**
- Strong technical fit with platform capabilities
- Clear ROI demonstration ($2.3M efficiency gains)
- Excellent relationship building throughout sales cycle
- Executive buy-in and budget approval
- Comprehensive technical evaluation completed

### 3. Lisa Thompson - New Inbound Lead (Early Stage Startup)
- **Lead ID**: Auto-generated
- **Email**: lisa.thompson@growthstartup.io
- **Company**: GrowthStartup Inc (25 employees, Series A)
- **Job Title**: Chief Operating Officer
- **Status**: NEW
- **Lead Score**: 45 (Early stage, needs nurturing)
- **Source**: Partner (StartupHub Accelerator referral)
- **Assigned To**: manager (Emma Davis)

**Business Context:**
- Early-stage startup in Series A funding round ($5M)
- Growth trajectory: 25 to 100+ employees over 18 months
- Budget constraints due to startup stage
- Has 20% partner discount from StartupHub Accelerator
- Decision timeline tied to funding completion (Q1 2025)

**Lead Characteristics:**
- Scalability-focused: needs CRM to support rapid growth
- Budget-conscious: interested in Starter plan with upgrade path
- Partner-referred: benefits from accelerator relationship
- Growth potential: could become significant customer as company scales

## Demo Notes Created

### David Kim - Technical Requirements & Discovery
1. **Initial Discovery Note** (Pinned):
   - Discovery call results with technical requirements
   - Budget range: $75K-$100K annually
   - Decision committee: David (technical), CFO (budget), CEO (approval)
   - Timeline: Decision by end of February 2025

2. **Technical Requirements Note**:
   - Must-haves: REST API, SAML SSO, advanced reporting, data export, SOC2
   - Nice-to-haves: Mobile app, webhooks, custom fields
   - POC environment requested for 2-week evaluation

### Michael Rodriguez - Conversion Success Story
3. **Conversion Success Note** (Pinned):
   - Complete lead-to-customer journey documentation
   - Success factors: technical fit, ROI demonstration, relationship building
   - Annual contract value: $180K
   - Validates enterprise sales process effectiveness

### Lisa Thompson - Partner Referral Follow-up
4. **Partner Referral Note**:
   - StartupHub Accelerator referral details
   - Startup stage: Series A, 25 employees, $5M funding round
   - Budget constraints and 20% partner discount
   - Growth timeline: 25 to 100+ employees over 18 months

## Demo Activities Created

### David Kim Activities
1. **Lead Creation Activity** (LEAD_CREATED):
   - Created from customer referral by Global Retail Solutions
   - Michael Rodriguez recommendation during quarterly review
   - Metadata includes referral source and incentive details

2. **Lead Qualification Activity** (STAGE_CHANGED):
   - Status change from CONTACTED to QUALIFIED
   - Score increase from 65 to 92
   - Qualification criteria: budget authority, technical need, timeline, company size

3. **Follow-up Scheduled Activity** (FOLLOWUP_SCHEDULED):
   - Technical demo scheduled for January 28, 2025
   - Decision committee members invited
   - Agenda: API demo, SSO integration, analytics walkthrough, POC planning

### Michael Rodriguez Activities
4. **Lead Conversion Activity** (LEAD_CONVERTED):
   - Successful conversion to Enterprise customer
   - Contract value: $180K annual
   - 75-day conversion cycle from inquiry to signed contract
   - Demonstrates effective enterprise sales process

### Lisa Thompson Activities
5. **Partner Referral Activity** (LEAD_CREATED):
   - Created from StartupHub Accelerator partner program
   - Partner contact: Jennifer Park
   - Discount code: STARTUP20
   - Company stage: Series A

6. **Initial Outreach Activity** (FOLLOWUP_SCHEDULED):
   - Qualification call scheduled for January 27, 2025
   - Focus: needs assessment, budget discussion, timeline planning
   - Plan recommendation: Starter with upgrade path

## Lead Scoring & Status Progression

### Lead Score Distribution
- **David Kim**: 92 (High-value, ready for conversion)
- **Michael Rodriguez**: 95 (Converted customer, highest value)
- **Lisa Thompson**: 45 (Early stage, needs nurturing)

### Status Flow Demonstration
```
NEW (Lisa) → Qualification → CONTACTED → QUALIFIED (David) → CONVERTED (Michael)
```

### Lead Sources Represented
- **Referral**: Customer-driven referrals (highest quality)
- **Website**: Inbound marketing leads (good conversion rate)
- **Partner**: Accelerator/partner referrals (growth potential)

## Business Scenarios Demonstrated

### 1. Customer Referral Program Success
- **David Kim**: High-value referral from satisfied customer
- **Referral Quality**: 92 lead score, qualified status
- **Referral Process**: Customer advocacy driving new business
- **ROI**: Demonstrates value of customer success investment

### 2. Lead-to-Customer Conversion
- **Michael Rodriguez**: Complete conversion journey
- **Conversion Metrics**: 75-day cycle, $180K contract value
- **Success Factors**: Technical fit, ROI demonstration, relationship building
- **Process Validation**: Proves enterprise sales methodology effectiveness

### 3. Partner Channel Development
- **Lisa Thompson**: Partner referral program results
- **Partner Value**: StartupHub Accelerator relationship
- **Growth Potential**: Early-stage startup with scaling needs
- **Channel Strategy**: Partner discounts and co-marketing

### 4. Lead Scoring & Qualification
- **Scoring Model**: 0-100 scale with qualification threshold at 60
- **Qualification Criteria**: Budget, Authority, Need, Timeline (BANT)
- **Score Progression**: Shows lead development over time
- **Automated Workflows**: Score-based lead routing and prioritization

## Integration with Other Services

### Contact Service Integration
- **Michael Rodriguez**: Lead ID 12345 converted to contact in contact service
- **Conversion Tracking**: `converted_to_contact_id` links lead to contact
- **Data Consistency**: Same person exists as both converted lead and active contact
- **Cross-Service Workflow**: Demonstrates lead-to-contact conversion process

### User Service Integration
- **Lead Assignment**: Leads assigned to demo users (admin, billing, manager)
- **Activity Tracking**: Activities created by and assigned to specific users
- **Permission Model**: Different users handle different lead types
- **Audit Trail**: Complete user attribution for all lead interactions

### Billing Service Integration
- **Revenue Attribution**: Converted leads linked to subscription revenue
- **Customer Success**: Michael Rodriguez represents $180K annual contract
- **Pipeline Value**: David Kim represents $75K+ potential revenue
- **Growth Tracking**: Lisa Thompson represents expansion market opportunity

## Testing Scenarios

### Lead Management Testing
- Test lead CRUD operations with various statuses and scores
- Verify lead scoring calculations and automatic updates
- Test lead assignment and reassignment workflows
- Validate lead search and filtering capabilities

### Lead Conversion Testing
- Test lead-to-contact conversion workflow
- Verify conversion timestamp and contact ID tracking
- Test conversion rollback scenarios
- Validate cross-service integration reliability

### Lead Scoring Testing
- Test automatic lead scoring based on engagement
- Verify manual score updates and thresholds
- Test qualification/disqualification workflows
- Validate score-based lead routing

### Activity & Note Management Testing
- Test activity logging for all lead interactions
- Verify note creation, updates, and pinning
- Test activity metadata storage and retrieval
- Validate audit trail completeness

### Lead Source Analytics Testing
- Test lead source performance tracking
- Verify conversion rates by source
- Test ROI calculations per channel
- Validate partner referral attribution

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
Use the demo lead records to test:
- Lead management APIs and bulk operations
- Lead scoring and qualification workflows
- Lead conversion and contact creation
- Activity logging and note management
- Lead source analytics and reporting

## API Testing Examples

### Lead Operations
```bash
# Get qualified leads
GET /api/v1/leads?status=QUALIFIED&minScore=80

# Search for InnovateTech leads
GET /api/v1/leads?search=innovatetech

# Get leads by source
GET /api/v1/leads?source=Referral

# Update lead score
PATCH /api/v1/leads/{david-kim-id}/score
```

### Lead Conversion
```bash
# Convert lead to contact
POST /api/v1/leads/{david-kim-id}/convert
{
  "companyId": 123,
  "notes": "Converted after successful POC"
}

# Get conversion history
GET /api/v1/leads?status=CONVERTED
```

### Lead Analytics
```bash
# Get lead counts by status
GET /api/v1/leads/analytics/status-counts

# Get lead counts by source
GET /api/v1/leads/analytics/source-counts

# Get conversion metrics
GET /api/v1/leads/analytics/conversion-metrics
```

### Notes & Activities
```bash
# Get lead notes
GET /api/v1/leads/{lead-id}/notes

# Add lead note
POST /api/v1/leads/{lead-id}/notes
{
  "content": "Follow-up call scheduled",
  "isPinned": false
}

# Get lead activities
GET /api/v1/leads/{lead-id}/activities
```

## Rollback Support

All demo migrations include comprehensive rollback scripts that remove data in dependency order:
1. Lead activities (foreign key to leads)
2. Lead notes (foreign key to leads)
3. Leads
4. Tenant info

```bash
# Rollback demo migrations
mvn liquibase:rollback -Dliquibase.rollbackCount=2
```

## Performance Considerations

### Indexing Strategy
- Email index for unique constraint and lookups
- Status index for pipeline filtering
- Score index for qualification queries
- Source index for analytics
- Assigned_to index for user-specific queries
- Created_at index for time-based reporting

### Caching Strategy
- Hibernate second-level cache for lead entities
- Query result caching for frequently accessed data
- Lead source reference data caching
- Activity type enumeration caching

### Scalability Features
- Pagination support for all list operations
- Bulk operations for lead imports
- Asynchronous activity logging
- Event-driven architecture for cross-service integration