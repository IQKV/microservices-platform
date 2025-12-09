# Billing Service API Documentation

## Overview

The Billing Service provides comprehensive subscription management, payment processing, invoice generation, and usage metering capabilities. This document provides detailed API endpoint documentation with request/response examples.

**Base URL**: `http://localhost:8082/api/v1`

**Authentication**: Most endpoints require JWT authentication via `Authorization: Bearer <token>` header.

## Table of Contents

1. [Subscription Plans](#subscription-plans)
2. [Subscriptions](#subscriptions)
3. [Payments](#payments)
4. [Payment Methods](#payment-methods)
5. [Invoices](#invoices)
6. [Usage Tracking](#usage-tracking)
7. [Customer Portal](#customer-portal)
8. [Admin Operations](#admin-operations)
9. [Internal APIs](#internal-apis)
10. [Webhooks](#webhooks)

---

## Subscription Plans

### List Available Plans

**Endpoint**: `GET /plans`

**Authentication**: None (public endpoint)

**Description**: Retrieve all active subscription plans available for purchase.

**Response**: `200 OK`

```json
[
  {
    "id": 1,
    "code": "FREE",
    "name": "Free Plan",
    "description": "Basic features for individuals",
    "tier": "FREE",
    "basePrice": 0.00,
    "currency": "USD",
    "billingCycle": "MONTHLY",
    "trialDays": 0,
    "features": {
      "api_access": true,
      "basic_support": true,
      "advanced_workflows": false
    },
    "quotas": {
      "API_CALLS": 1000,
      "STORAGE_GB": 1,
      "EMAIL_SENDS": 100,
      "ACTIVE_USERS": 1
    },
    "active": true
  },
  {
    "id": 2,
    "code": "PRO",
    "name": "Professional Plan",
    "description": "Advanced features for growing teams",
    "tier": "PRO",
    "basePrice": 49.00,
    "currency": "USD",
    "billingCycle": "MONTHLY",
    "trialDays": 14,
    "features": {
      "api_access": true,
      "basic_support": true,
      "advanced_workflows": true,
      "priority_support": true,
      "custom_integrations": true
    },
    "quotas": {
      "API_CALLS": 50000,
      "STORAGE_GB": 50,
      "EMAIL_SENDS": 10000,
      "ACTIVE_USERS": 10
    },
    "active": true
  }
]
```

### Get Plan Details

**Endpoint**: `GET /plans/{id}`

**Authentication**: None (public endpoint)

**Path Parameters**:
- `id` (integer, required): Plan ID

**Response**: `200 OK`

```json
{
  "id": 2,
  "code": "PRO",
  "name": "Professional Plan",
  "description": "Advanced features for growing teams",
  "tier": "PRO",
  "basePrice": 49.00,
  "currency": "USD",
  "billingCycle": "MONTHLY",
  "trialDays": 14,
  "features": {
    "api_access": true,
    "advanced_workflows": true,
    "priority_support": true
  },
  "quotas": {
    "API_CALLS": 50000,
    "STORAGE_GB": 50,
    "EMAIL_SENDS": 10000
  }
}
```

---

## Subscriptions

### Create Subscription

**Endpoint**: `POST /subscriptions`

**Authentication**: Required

**Description**: Create a new subscription for the authenticated tenant.

**Request Body**:

```json
{
  "planId": 2,
  "paymentMethodId": "pm_1234567890",
  "startTrial": true
}
```

**Response**: `201 Created`

```json
{
  "id": 123,
  "tenantId": "tenant-uuid",
  "planId": 2,
  "planCode": "PRO",
  "status": "TRIAL",
  "currentPeriodStart": "2024-12-09T00:00:00Z",
  "currentPeriodEnd": "2024-12-23T00:00:00Z",
  "trialStart": "2024-12-09T00:00:00Z",
  "trialEnd": "2024-12-23T00:00:00Z",
  "cancelAtPeriodEnd": false,
  "createdAt": "2024-12-09T10:30:00Z"
}
```

### Get My Subscription

**Endpoint**: `GET /subscriptions/me`

**Authentication**: Required

**Response**: `200 OK`

```json
{
  "id": 123,
  "tenantId": "tenant-uuid",
  "planId": 2,
  "planCode": "PRO",
  "planName": "Professional Plan",
  "status": "ACTIVE",
  "currentPeriodStart": "2024-12-01T00:00:00Z",
  "currentPeriodEnd": "2025-01-01T00:00:00Z",
  "cancelAtPeriodEnd": false,
  "trialEnd": null,
  "features": {
    "api_access": true,
    "advanced_workflows": true,
    "priority_support": true
  },
  "quotas": {
    "API_CALLS": 50000,
    "STORAGE_GB": 50,
    "EMAIL_SENDS": 10000
  }
}
```

### Upgrade Subscription

**Endpoint**: `POST /subscriptions/{id}/upgrade`

**Authentication**: Required

**Path Parameters**:
- `id` (integer, required): Subscription ID

**Request Body**:

```json
{
  "newPlanId": 3,
  "effectiveDate": "IMMEDIATE"
}
```

**Response**: `200 OK`

```json
{
  "subscription": {
    "id": 123,
    "planId": 3,
    "planCode": "ENTERPRISE",
    "status": "ACTIVE"
  },
  "prorationInvoice": {
    "id": 456,
    "amount": 150.00,
    "status": "PAID",
    "lineItems": [
      {
        "description": "Credit for unused time on PRO plan",
        "amount": -25.00
      },
      {
        "description": "Charge for ENTERPRISE plan (prorated)",
        "amount": 175.00
      }
    ]
  }
}
```

### Cancel Subscription

**Endpoint**: `POST /subscriptions/{id}/cancel`

**Authentication**: Required

**Request Body**:

```json
{
  "cancelImmediately": false,
  "reason": "Switching to competitor"
}
```

**Response**: `200 OK`

```json
{
  "id": 123,
  "status": "ACTIVE",
  "cancelAtPeriodEnd": true,
  "canceledAt": "2024-12-09T10:30:00Z",
  "currentPeriodEnd": "2025-01-01T00:00:00Z",
  "message": "Subscription will be canceled at period end"
}
```

---

## Payments

### Process Payment

**Endpoint**: `POST /payments`

**Authentication**: Required

**Description**: Process a payment for an invoice with idempotency support.

**Request Body**:

```json
{
  "invoiceId": 789,
  "paymentMethodId": "pm_1234567890",
  "amount": 49.00,
  "currency": "USD",
  "idempotencyKey": "payment-123-retry-1"
}
```

**Response**: `200 OK`

```json
{
  "id": 1001,
  "invoiceId": 789,
  "amount": 49.00,
  "currency": "USD",
  "status": "SUCCEEDED",
  "paymentMethod": "card",
  "last4": "4242",
  "transactionId": "ch_1234567890",
  "processedAt": "2024-12-09T10:30:00Z"
}
```

**Error Response**: `402 Payment Required`

```json
{
  "error": "payment_failed",
  "message": "Payment declined by card issuer",
  "paymentId": 1001,
  "declineCode": "insufficient_funds"
}
```

### Retry Failed Payment

**Endpoint**: `POST /payments/{id}/retry`

**Authentication**: Required

**Response**: `200 OK`

```json
{
  "id": 1001,
  "status": "SUCCEEDED",
  "retryAttempt": 2,
  "processedAt": "2024-12-09T11:00:00Z"
}
```

### Refund Payment

**Endpoint**: `POST /payments/{id}/refund`

**Authentication**: Required

**Request Body**:

```json
{
  "amount": 49.00,
  "reason": "Customer request"
}
```

**Response**: `200 OK`

```json
{
  "id": 1002,
  "originalPaymentId": 1001,
  "amount": 49.00,
  "status": "REFUNDED",
  "refundedAt": "2024-12-09T12:00:00Z"
}
```

---

## Payment Methods

### Add Payment Method

**Endpoint**: `POST /payment-methods`

**Authentication**: Required

**Request Body**:

```json
{
  "type": "CARD",
  "token": "tok_visa",
  "setAsDefault": true
}
```

**Response**: `201 Created`

```json
{
  "id": "pm_1234567890",
  "type": "CARD",
  "brand": "visa",
  "last4": "4242",
  "expiryMonth": 12,
  "expiryYear": 2025,
  "isDefault": true,
  "createdAt": "2024-12-09T10:30:00Z"
}
```

### List Payment Methods

**Endpoint**: `GET /payment-methods`

**Authentication**: Required

**Response**: `200 OK`

```json
[
  {
    "id": "pm_1234567890",
    "type": "CARD",
    "brand": "visa",
    "last4": "4242",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "isDefault": true
  },
  {
    "id": "pm_0987654321",
    "type": "CARD",
    "brand": "mastercard",
    "last4": "5555",
    "expiryMonth": 6,
    "expiryYear": 2026,
    "isDefault": false
  }
]
```

### Remove Payment Method

**Endpoint**: `DELETE /payment-methods/{id}`

**Authentication**: Required

**Response**: `204 No Content`

---

## Invoices

### List Invoices

**Endpoint**: `GET /invoices`

**Authentication**: Required

**Query Parameters**:
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20)
- `status` (string, optional): Filter by status (DRAFT, OPEN, PAID, VOID)

**Response**: `200 OK`

```json
{
  "content": [
    {
      "id": 789,
      "invoiceNumber": "INV-202412-00789",
      "status": "PAID",
      "amount": 49.00,
      "currency": "USD",
      "dueDate": "2024-12-16T00:00:00Z",
      "paidAt": "2024-12-09T10:30:00Z",
      "lineItems": [
        {
          "description": "PRO Plan - Monthly",
          "quantity": 1,
          "unitPrice": 49.00,
          "amount": 49.00
        }
      ]
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### Get Invoice Details

**Endpoint**: `GET /invoices/{id}`

**Authentication**: Required

**Response**: `200 OK`

```json
{
  "id": 789,
  "invoiceNumber": "INV-202412-00789",
  "subscriptionId": 123,
  "status": "PAID",
  "subtotal": 49.00,
  "tax": 0.00,
  "total": 49.00,
  "currency": "USD",
  "dueDate": "2024-12-16T00:00:00Z",
  "paidAt": "2024-12-09T10:30:00Z",
  "lineItems": [
    {
      "description": "PRO Plan - Monthly",
      "quantity": 1,
      "unitPrice": 49.00,
      "amount": 49.00
    }
  ],
  "payments": [
    {
      "id": 1001,
      "amount": 49.00,
      "status": "SUCCEEDED",
      "processedAt": "2024-12-09T10:30:00Z"
    }
  ]
}
```

### Download Invoice PDF

**Endpoint**: `GET /invoices/{id}/pdf`

**Authentication**: Required

**Response**: `200 OK` (application/pdf)

---

## Usage Tracking

### Get Usage Summary

**Endpoint**: `GET /usage/summary`

**Authentication**: Required

**Query Parameters**:
- `startDate` (string, optional): Start date (ISO 8601)
- `endDate` (string, optional): End date (ISO 8601)

**Response**: `200 OK`

```json
{
  "period": {
    "start": "2024-12-01T00:00:00Z",
    "end": "2025-01-01T00:00:00Z"
  },
  "metrics": [
    {
      "type": "API_CALLS",
      "currentUsage": 12500,
      "limit": 50000,
      "remaining": 37500,
      "percentageUsed": 25.0,
      "isApproachingLimit": false
    },
    {
      "type": "STORAGE_GB",
      "currentUsage": 15,
      "limit": 50,
      "remaining": 35,
      "percentageUsed": 30.0,
      "isApproachingLimit": false
    },
    {
      "type": "EMAIL_SENDS",
      "currentUsage": 8500,
      "limit": 10000,
      "remaining": 1500,
      "percentageUsed": 85.0,
      "isApproachingLimit": true
    }
  ]
}
```

### Check Quota

**Endpoint**: `GET /usage/quota`

**Authentication**: Required

**Query Parameters**:
- `metricType` (string, required): Metric type (API_CALLS, STORAGE_GB, etc.)

**Response**: `200 OK`

```json
{
  "metricType": "API_CALLS",
  "allowed": true,
  "currentUsage": 12500,
  "limit": 50000,
  "remaining": 37500,
  "percentageUsed": 25.0,
  "graceLimit": 52500,
  "isApproachingLimit": false,
  "message": "Quota available"
}
```

---

## Customer Portal

### Get Billing Dashboard

**Endpoint**: `GET /portal/dashboard`

**Authentication**: Required

**Response**: `200 OK`

```json
{
  "subscription": {
    "id": 123,
    "planName": "Professional Plan",
    "status": "ACTIVE",
    "currentPeriodEnd": "2025-01-01T00:00:00Z",
    "cancelAtPeriodEnd": false
  },
  "usage": {
    "API_CALLS": {
      "used": 12500,
      "limit": 50000,
      "percentage": 25.0
    },
    "STORAGE_GB": {
      "used": 15,
      "limit": 50,
      "percentage": 30.0
    }
  },
  "upcomingInvoice": {
    "amount": 49.00,
    "dueDate": "2025-01-01T00:00:00Z"
  },
  "paymentMethod": {
    "type": "CARD",
    "last4": "4242",
    "brand": "visa"
  }
}
```

---

## Admin Operations

### List All Subscriptions

**Endpoint**: `GET /admin/subscriptions`

**Authentication**: Required (ADMIN/SUPER_ADMIN)

**Query Parameters**:
- `page` (integer, optional): Page number
- `size` (integer, optional): Page size
- `status` (string, optional): Filter by status

**Response**: `200 OK`

```json
{
  "content": [
    {
      "id": 123,
      "tenantId": "tenant-uuid-1",
      "planCode": "PRO",
      "status": "ACTIVE",
      "currentPeriodEnd": "2025-01-01T00:00:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0
}
```

### Revenue Report

**Endpoint**: `GET /admin/analytics/revenue`

**Authentication**: Required (ADMIN/SUPER_ADMIN)

**Query Parameters**:
- `startDate` (string, required): Start date
- `endDate` (string, required): End date

**Response**: `200 OK`

```json
{
  "period": {
    "start": "2024-11-01T00:00:00Z",
    "end": "2024-12-01T00:00:00Z"
  },
  "totalRevenue": 15000.00,
  "mrr": 12500.00,
  "arr": 150000.00,
  "newSubscriptions": 25,
  "canceledSubscriptions": 5,
  "churnRate": 3.2,
  "revenueByPlan": {
    "FREE": 0.00,
    "PRO": 9800.00,
    "ENTERPRISE": 5200.00
  }
}
```

---

## Internal APIs

### Check Quota (Internal)

**Endpoint**: `POST /internal/quota/check`

**Authentication**: Required (Service-to-Service)

**Description**: Check if tenant has quota available for an operation.

**Request Body**:

```json
{
  "tenantId": "tenant-uuid",
  "metricType": "API_CALLS",
  "quantity": 1
}
```

**Response**: `200 OK`

```json
{
  "allowed": true,
  "currentUsage": 12500,
  "limit": 50000,
  "remaining": 37500,
  "message": "Quota available"
}
```

### Record Usage (Internal)

**Endpoint**: `POST /internal/usage/record`

**Authentication**: Required (Service-to-Service)

**Request Body**:

```json
{
  "tenantId": "tenant-uuid",
  "metricType": "API_CALLS",
  "quantity": 1,
  "metadata": {
    "endpoint": "/api/v1/contacts",
    "method": "POST"
  }
}
```

**Response**: `202 Accepted`

```json
{
  "recorded": true,
  "currentUsage": 12501
}
```

---

## Webhooks

### Stripe Webhook

**Endpoint**: `POST /webhooks/stripe`

**Authentication**: Webhook signature verification

**Description**: Handles Stripe webhook events for payment processing.

**Supported Events**:
- `payment_intent.succeeded`
- `payment_intent.failed`
- `charge.refunded`
- `customer.subscription.updated`

### PayPal Webhook

**Endpoint**: `POST /webhooks/paypal`

**Authentication**: Webhook signature verification

**Description**: Handles PayPal webhook events for payment processing.

---

## Error Responses

### Standard Error Format

All error responses follow RFC 7807 Problem Details format:

```json
{
  "type": "https://api.iqscaffold.com/errors/quota-exceeded",
  "title": "Quota Exceeded",
  "status": 429,
  "detail": "API call quota exceeded. Current usage: 50000/50000",
  "instance": "/api/v1/internal/quota/check",
  "timestamp": "2024-12-09T10:30:00Z",
  "traceId": "abc123"
}
```

### Common Error Codes

- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource conflict (e.g., duplicate subscription)
- `422 Unprocessable Entity`: Validation error
- `429 Too Many Requests`: Rate limit exceeded or quota exceeded
- `500 Internal Server Error`: Server error
- `502 Bad Gateway`: Payment provider error
- `503 Service Unavailable`: Service temporarily unavailable

---

## Rate Limiting

All API endpoints are rate-limited per tenant:

- **Default Limit**: 100 requests per minute
- **Burst Capacity**: 200 requests
- **Header**: `X-RateLimit-Remaining` indicates remaining requests

**Rate Limit Exceeded Response**: `429 Too Many Requests`

```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests. Please try again later.",
  "retryAfter": 60
}
```

---

## Pagination

List endpoints support pagination with the following query parameters:

- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Paginated Response Format**:

```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## Authentication

### JWT Token Format

All authenticated requests require a JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Required JWT Claims

- `sub`: User ID
- `username`: Username
- `email`: User email
- `roles`: User authorities (ADMIN, SUPER_ADMIN, USER)
- `tenant_id`: Tenant ID
- `firstName`: User first name
- `lastName`: User last name

---

## OpenAPI Documentation

Interactive API documentation is available at:

- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8082/v3/api-docs`

---

## Support

For API support and questions:

- **Documentation**: https://docs.iqscaffold.com/billing
- **Support Email**: support@iqscaffold.com
- **Status Page**: https://status.iqscaffold.com
