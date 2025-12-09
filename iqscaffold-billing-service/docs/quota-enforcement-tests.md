# Quota Enforcement Tests

## Overview

Comprehensive unit tests for quota enforcement logic, covering various usage levels, exceeded scenarios, and grace period (5% overage) handling.

## Test Coverage Summary

- **Total Tests**: 47
- **Test Categories**: 9 nested test classes
- **All Tests Passing**: ✅

## Grace Period Logic

The quota enforcement system includes a **5% grace period** that allows users to slightly exceed their quota limits before hard enforcement kicks in.

**Formula**: `graceLimit = baseLimit * 1.05`

**Example**:

- Base limit: 1000 API calls
- Grace limit: 1050 API calls (5% overage allowed)
- Hard rejection: 1051+ API calls

## Test Categories

### 1. Various Usage Levels Tests (7 tests)

Tests quota checking at different usage percentages:

- ✅ 0% usage: Full quota available
- ✅ 25% usage: 75% remaining
- ✅ 50% usage: 50% remaining
- ✅ 75% usage: 25% remaining
- ✅ 90% usage: Approaching limit warning
- ✅ 95% usage: Within grace period
- ✅ 100% usage: At limit, grace period applies

### 2. Grace Period Logic Tests (6 tests)

Tests the 5% overage allowance:

- ✅ Exactly at grace limit (1050/1050): Allowed
- ✅ Just over grace limit (1051/1050): Denied
- ✅ Just under grace limit (1049/1050): Allowed
- ✅ Grace limit calculation for different quotas
- ✅ Grace period applies to all metric types
- ✅ No grace period for unlimited quotas

### 3. Quota Exceeded Scenarios (6 tests)

Tests various exceeded conditions:

- ✅ Significantly over limit: Denied
- ✅ Already over grace limit: Denied
- ✅ Exception thrown when enforcing exceeded quota
- ✅ Detailed error message provided
- ✅ Projected usage exceeds grace limit: Denied
- ✅ hasQuotaAvailable returns false when exceeded

### 4. Edge Cases Tests (6 tests)

Tests boundary conditions:

- ✅ Zero requested amount
- ✅ Exactly at limit
- ✅ One unit over limit (within grace)
- ✅ Large requested amount
- ✅ Very small quotas (1 GB storage)
- ✅ Fractional grace period handling

### 5. Multiple Metric Types Tests (5 tests)

Tests different quota types:

- ✅ API_CALLS quota enforcement
- ✅ STORAGE_GB quota enforcement
- ✅ EMAIL_SENDS quota enforcement
- ✅ ACTIVE_USERS quota enforcement
- ✅ Independent quota tracking per metric

### 6. Unlimited Quota Tests (5 tests)

Tests unlimited (enterprise) quotas:

- ✅ Always allows regardless of usage
- ✅ Returns zero percentage used
- ✅ Never approaches limit
- ✅ Always has quota available
- ✅ Returns Long.MAX_VALUE for remaining quota

### 7. Quota Status and Monitoring Tests (5 tests)

Tests monitoring and alerting:

- ✅ Approaching limit detection at 90%
- ✅ Not flagged below 90%
- ✅ Accurate usage percentage calculation
- ✅ Remaining quota calculation at various levels
- ✅ Quota availability checking

### 8. Different Plan Tiers Tests (3 tests)

Tests quota enforcement across plan tiers:

- ✅ FREE tier limits enforced correctly
- ✅ PRO tier has higher limits
- ✅ ENTERPRISE tier has unlimited quotas

### 9. Boundary Condition Tests (4 tests)

Tests extreme boundary values:

- ✅ Minimum values (zero usage, zero request)
- ✅ Maximum single request within limit
- ✅ Request exactly at grace boundary
- ✅ Request one over grace boundary

## Quota Limits by Plan Tier

### FREE Tier

- API Calls: 1,000/month (grace: 1,050)
- Storage: 1 GB (grace: 1.05 GB → 1 GB due to rounding)
- Email Sends: 100/month (grace: 105)
- Active Users: 5 (grace: 5.25 → 5 due to rounding)

### PRO Tier

- Higher limits than FREE (tested dynamically)
- Grace period applies to all quotas

### ENTERPRISE Tier

- Unlimited quotas (no limits)
- No grace period needed

## Key Test Scenarios

### Scenario 1: Normal Usage

```java
currentUsage = 500L;  // 50% of 1000
requestedAmount = 100L;
result = quotaEnforcer.checkQuota(subscription, MetricType.API_CALLS, 500L, 100L);
// Result: ALLOWED, 400 remaining
```

### Scenario 2: At Limit with Grace

```java
currentUsage = 1000L;  // 100% of 1000
requestedAmount = 30L;  // Within 5% grace
result = quotaEnforcer.checkQuota(subscription, MetricType.API_CALLS, 1000L, 30L);
// Result: ALLOWED (grace period), 20 remaining in grace
```

### Scenario 3: Exceeded Grace Limit

```java
currentUsage = 1000L;  // 100% of 1000
requestedAmount = 100L;  // Exceeds 5% grace (1100 > 1050)
result = quotaEnforcer.checkQuota(subscription, MetricType.API_CALLS, 1000L, 100L);
// Result: DENIED, 0 remaining
```

### Scenario 4: Approaching Limit Warning

```java
currentUsage = 920L;  // 92% of 1000
isApproaching = quotaEnforcer.isApproachingLimit(subscription, MetricType.API_CALLS, 920L);
// Result: TRUE (>= 90% threshold)
```

## Grace Period Calculation Examples

| Base Limit | Grace Limit (5%) | Allowed Range | Hard Rejection |
| ---------- | ---------------- | ------------- | -------------- |
| 1,000      | 1,050            | 0-1,050       | 1,051+         |
| 100        | 105              | 0-105         | 106+           |
| 10         | 10.5 → 10        | 0-10          | 11+            |
| 5          | 5.25 → 5         | 0-5           | 6+             |
| 1          | 1.05 → 1         | 0-1           | 2+             |

**Note**: Grace limits are cast to `long`, so fractional values are truncated.

## Test File Location

`backend/iqscaffold-billing-service/src/test/java/com/iqscaffold/billingservice/usage/QuotaEnforcementTest.java`

## Running the Tests

```bash
# Run only quota enforcement tests
mvn test -Dtest=QuotaEnforcementTest

# Run all quota-related tests
mvn test -Dtest=Quota*Test
```

## Design Notes

- Tests use JUnit 5 nested test classes for logical grouping
- Each usage level has dedicated tests
- Grace period logic is thoroughly tested with boundary conditions
- Multiple metric types are tested to ensure consistent behavior
- Unlimited quotas are tested separately
- All tests use realistic quota values from actual plan tiers
