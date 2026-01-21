# Pipeline Service Demo Data

This directory contains demo data migrations for the Pipeline Service, providing realistic pipeline records for development and testing purposes.

## Demo Scenario Overview

The demo data creates a comprehensive pipeline scenario with 3 pipeline items representing different stages of the sales process, along with associated follow-ups and activities.

### Demo Tenant Context
- **Tenant ID**: `demo-tenant`
- **Tenant Name**: Demo Tech Solutions
- **Schema**: `demo_tenant`
- **Status**: ACTIVE

## Pipeline Items Created

### 1. David Kim - High-Value Qualified Lead
- **Lead ID**: 1001 (references lead service)
- **Stage**: Qualified
- **Expected Value**: $85,000
- **Probability**: 75%
- **Days in Stage**: 34 days
- **Status**: Active, high-priority prospect
- **Description**: Enterprise client with confirmed budget and identified decision maker. Represents a typical qualified lead ready for proposal presentation.

### 2. Michael Rodriguez - Converted Customer
- **Lead ID**: 1002 (references lead service)
- **Stage**: Won
- **Expected Value**: $180,000
- **Probability**: 100%
- **Days in Stage**: 67 days
- **Status**: Successfully converted
- **Conversion Date**: 2024-11-15
- **Description**: Successfully converted customer with signed contract and processed payment. Demonstrates complete pipeline conversion flow.

### 3. Lisa Thompson - New Startup Lead
- **Lead ID**: 1003 (references lead service)
- **Stage**: New
- **Expected Value**: $25,000
- **Probability**: 25%
- **Days in Stage**: 1 day
- **Status**: Fresh lead requiring discovery
- **Description**: New startup lead with budget constraints but growth potential. Represents early-stage pipeline entry.

## Follow-ups Created

### David Kim Follow-ups
1. **Proposal Presentation** (High Priority)
   - Scheduled: 2025-01-25 14:00 UTC
   - Assigned to: admin
   - Focus: Comprehensive solution proposal with enterprise features

2. **Technical Requirements Review** (Medium Priority)
   - Scheduled: 2025-01-28 10:30 UTC
   - Assigned to: manager
   - Focus: Technical integration and security compliance

### Lisa Thompson Follow-ups
1. **Discovery Call** (Medium Priority)
   - Scheduled: 2025-01-23 15:00 UTC
   - Assigned to: manager
   - Focus: Understanding startup needs and budget constraints

## Pipeline Activities Created

### David Kim Activities
- **Stage Change**: Moved from 'Contacted' to 'Qualified' (2024-12-18)
- **Value Update**: Increased from $75,000 to $85,000 due to expanded scope (2025-01-10)

### Michael Rodriguez Activities
- **Conversion**: Successfully converted with contract signing (2024-11-15)
- **Stage Change**: Moved to 'Won' stage with 100% conversion rate (2024-11-15)

### Lisa Thompson Activities
- **Lead Created**: Initial pipeline entry from website form (2025-01-20)

## Integration with Other Services

### Lead Service Integration
- Pipeline items reference lead IDs (1001, 1002, 1003) from the lead service
- Maintains consistency with lead scoring and status
- Conversion tracking aligns with lead conversion data

### Contact Service Integration
- Michael Rodriguez (lead 1002) corresponds to converted contact in contact service
- Pipeline conversion aligns with contact status changes

### User Service Integration
- Activities performed by users with appropriate authorities:
  - `admin`: High-level pipeline management
  - `manager`: Lead qualification and follow-ups
  - `billing`: Conversion and payment processing

## Usage Instructions

### Running Demo Migrations
```bash
# Run with demo context to include demo data
mvn liquibase:update -Dcontexts=demo

# Run without demo context for production
mvn liquibase:update
```

### Demo Data Characteristics
- **Realistic Values**: Deal values and probabilities based on typical B2B scenarios
- **Time-based Progression**: Activities and stage changes follow logical timeline
- **Multi-stage Representation**: Covers New, Qualified, and Won stages
- **Complete Activity Trail**: Full audit trail of pipeline progression
- **Cross-service Consistency**: Aligns with lead, contact, and user service data

## Files Structure

```
demo/
├── master.xml                                    # Demo migrations master file
├── 20250121000000-demo-pipeline-records.xml     # Core pipeline items and tenant setup
├── 20250121000001-demo-pipeline-followups-activities.xml  # Follow-ups and activities
└── README.md                                     # This documentation
```

## Development Notes

- All demo data uses `context="demo"` to prevent accidental production deployment
- Pipeline items include proper rollback procedures
- Activities include metadata for detailed tracking
- Follow-ups demonstrate different priority levels and assignment patterns
- Values and dates are realistic and internally consistent

This demo data provides a comprehensive foundation for testing pipeline functionality, UI development, and integration testing across the microservices ecosystem.