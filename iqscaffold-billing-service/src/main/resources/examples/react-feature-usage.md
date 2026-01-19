# React Feature Usage Example

This document shows how React applications can consume the new Feature API endpoints to implement feature-based UI rendering.

## API Endpoints

### Get User Features (Complete)
```
GET /api/v1/features/my-features
```
Returns complete feature information including subscription details.

### Get Enabled Features Only (Lightweight)
```
GET /api/v1/features/enabled
```
Returns only enabled features for faster loading.

## Gateway Integration

The feature API endpoints are integrated with the Spring Cloud Gateway and include:

### Route Configuration
- **Feature Routes**: Optimized for high-frequency frontend calls
  - `/api/v1/features/my-features` - Rate limit: 100 req/min, burst: 150
  - `/api/v1/features/enabled` - Rate limit: 200 req/min, burst: 300
- **Circuit Breaker**: Automatic fallback when billing service is unavailable
- **Retry Logic**: Automatic retries for transient failures
- **Request Transformation**: Automatic header enrichment and tenant context

### Fallback Behavior
When the billing service is unavailable, the gateway returns:
```json
{
  "enabledFeatures": [],
  "allFeatures": [],
  "planName": "Service Unavailable",
  "subscriptionStatus": "UNKNOWN",
  "tenantId": "unknown",
  "error": "Feature service temporarily unavailable"
}
```

This allows React applications to continue functioning with graceful degradation.

## React Implementation Examples

### 1. Feature Hook

```typescript
// hooks/useFeatures.ts
import { useState, useEffect } from 'react';

interface Feature {
  code: string;
  name: string;
  description: string;
  category: string;
  enabled: boolean;
  usageLimit?: number;
  currentUsage?: number;
}

interface UserFeaturesResponse {
  enabledFeatures: Feature[];
  allFeatures: Feature[];
  planName: string;
  subscriptionStatus: string;
  subscriptionExpiresAt?: string;
  isTrialPeriod: boolean;
  trialExpiresAt?: string;
  tenantId: string;
}

export const useFeatures = () => {
  const [features, setFeatures] = useState<UserFeaturesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchFeatures = async () => {
      try {
        // API Gateway URL - routes to billing service
        const response = await fetch('/api/v1/features/my-features', {
          headers: {
            'Authorization': `Bearer ${getAuthToken()}`,
            'Content-Type': 'application/json',
            'X-Tenant-ID': getTenantId(), // Required for multi-tenant routing
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        
        // Handle fallback responses from gateway circuit breaker
        if (response.headers.get('X-Fallback-Response') === 'true') {
          console.warn('Feature service unavailable, using fallback data');
          setError('Feature service temporarily unavailable');
        }
        
        setFeatures(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch features');
      } finally {
        setLoading(false);
      }
    };

    fetchFeatures();
  }, []);

  const hasFeature = (featureCode: string): boolean => {
    return features?.enabledFeatures.some(f => f.code === featureCode) ?? false;
  };

  const getFeature = (featureCode: string): Feature | undefined => {
    return features?.enabledFeatures.find(f => f.code === featureCode);
  };

  const getUsageInfo = (featureCode: string) => {
    const feature = getFeature(featureCode);
    if (!feature || !feature.usageLimit) return null;
    
    return {
      current: feature.currentUsage || 0,
      limit: feature.usageLimit,
      percentage: ((feature.currentUsage || 0) / feature.usageLimit) * 100,
      remaining: feature.usageLimit - (feature.currentUsage || 0),
    };
  };

  return {
    features,
    loading,
    error,
    hasFeature,
    getFeature,
    getUsageInfo,
  };
};

// Helper functions to get auth token and tenant ID (implement based on your auth system)
const getAuthToken = (): string => {
  // Return JWT token from localStorage, cookies, or auth context
  return localStorage.getItem('authToken') || '';
};

const getTenantId = (): string => {
  // Return tenant ID from JWT token, localStorage, or auth context
  // This is extracted from JWT by the gateway and passed to services
  return localStorage.getItem('tenantId') || '';
};
```

### 2. Feature-Based Component Rendering

```typescript
// components/FeatureGate.tsx
import React from 'react';
import { useFeatures } from '../hooks/useFeatures';

interface FeatureGateProps {
  feature: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export const FeatureGate: React.FC<FeatureGateProps> = ({ 
  feature, 
  children, 
  fallback = null 
}) => {
  const { hasFeature, loading } = useFeatures();

  if (loading) {
    return <div>Loading...</div>;
  }

  return hasFeature(feature) ? <>{children}</> : <>{fallback}</>;
};
```

### 3. Usage Examples

```typescript
// components/Dashboard.tsx
import React from 'react';
import { FeatureGate } from './FeatureGate';
import { useFeatures } from '../hooks/useFeatures';

export const Dashboard: React.FC = () => {
  const { features, hasFeature, getUsageInfo } = useFeatures();

  return (
    <div className="dashboard">
      <h1>Dashboard</h1>
      
      {/* Basic feature gate */}
      <FeatureGate feature="advanced_analytics">
        <div className="analytics-section">
          <h2>Advanced Analytics</h2>
          <p>Premium analytics dashboard content...</p>
        </div>
      </FeatureGate>

      {/* Feature gate with fallback */}
      <FeatureGate 
        feature="premium_support" 
        fallback={
          <div className="upgrade-prompt">
            <p>Upgrade to Pro plan for premium support!</p>
            <button>Upgrade Now</button>
          </div>
        }
      >
        <div className="support-section">
          <h2>Premium Support</h2>
          <button>Contact Support</button>
        </div>
      </FeatureGate>

      {/* Usage-based feature with quota display */}
      {hasFeature('api_calls') && (
        <div className="api-usage">
          <h3>API Usage</h3>
          {(() => {
            const usage = getUsageInfo('api_calls');
            return usage ? (
              <div>
                <div className="usage-bar">
                  <div 
                    className="usage-fill" 
                    style={{ width: `${usage.percentage}%` }}
                  />
                </div>
                <p>{usage.current} / {usage.limit} calls used</p>
                <p>{usage.remaining} calls remaining</p>
              </div>
            ) : (
              <p>Unlimited API calls</p>
            );
          })()}
        </div>
      )}

      {/* Subscription info */}
      {features && (
        <div className="subscription-info">
          <h3>Subscription: {features.planName}</h3>
          <p>Status: {features.subscriptionStatus}</p>
          {features.isTrialPeriod && (
            <p className="trial-warning">
              Trial expires: {new Date(features.trialExpiresAt!).toLocaleDateString()}
            </p>
          )}
        </div>
      )}
    </div>
  );
};
```

### 4. Lightweight Feature Loading

For better performance, use the lightweight endpoint for simple feature checks:

```typescript
// hooks/useEnabledFeatures.ts
import { useState, useEffect } from 'react';

export const useEnabledFeatures = () => {
  const [enabledFeatures, setEnabledFeatures] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchEnabledFeatures = async () => {
      try {
        const response = await fetch('/api/v1/features/enabled', {
          headers: {
            'Authorization': `Bearer ${getAuthToken()}`,
            'Content-Type': 'application/json',
          },
        });

        if (response.ok) {
          const features = await response.json();
          setEnabledFeatures(features.map((f: any) => f.code));
        }
      } catch (err) {
        console.error('Failed to fetch enabled features:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchEnabledFeatures();
  }, []);

  const hasFeature = (featureCode: string): boolean => {
    return enabledFeatures.includes(featureCode);
  };

  return { enabledFeatures, hasFeature, loading };
};
```

### 5. Feature-Based Navigation

```typescript
// components/Navigation.tsx
import React from 'react';
import { useEnabledFeatures } from '../hooks/useEnabledFeatures';

export const Navigation: React.FC = () => {
  const { hasFeature } = useEnabledFeatures();

  return (
    <nav>
      <ul>
        <li><a href="/dashboard">Dashboard</a></li>
        <li><a href="/contacts">Contacts</a></li>
        
        {hasFeature('advanced_analytics') && (
          <li><a href="/analytics">Analytics</a></li>
        )}
        
        {hasFeature('lead_management') && (
          <li><a href="/leads">Lead Management</a></li>
        )}
        
        {hasFeature('pipeline_management') && (
          <li><a href="/pipeline">Sales Pipeline</a></li>
        )}
        
        {hasFeature('reporting') && (
          <li><a href="/reports">Reports</a></li>
        )}
      </ul>
    </nav>
  );
};
```

### 6. Error Handling

```typescript
// components/FeatureErrorBoundary.tsx
import React from 'react';

interface Props {
  children: React.ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class FeatureErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Feature loading error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="feature-error">
          <h2>Something went wrong loading features</h2>
          <p>Please refresh the page or contact support if the problem persists.</p>
          <button onClick={() => window.location.reload()}>
            Refresh Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
```

## Best Practices

### 1. Caching
- Cache feature data in React context or state management library
- Refresh features when subscription changes
- Use lightweight endpoint for frequent checks

### 2. Performance
- Load features early in app initialization
- Use feature gates to conditionally load heavy components
- Implement loading states for better UX

### 3. Error Handling
- Gracefully handle API failures
- Provide fallback UI when features can't be loaded
- Log feature-related errors for debugging

### 4. Security
- Always include authentication headers
- Validate feature access on the backend
- Don't rely solely on frontend feature gates for security

## Example API Responses

### Complete Features Response
```json
{
  "enabledFeatures": [
    {
      "code": "advanced_analytics",
      "name": "Advanced Analytics",
      "description": "Access to advanced reporting and analytics",
      "category": "ANALYTICS",
      "enabled": true,
      "usageLimit": null,
      "currentUsage": 0
    },
    {
      "code": "api_calls",
      "name": "API Calls",
      "description": "Monthly API call quota",
      "category": "API",
      "enabled": true,
      "usageLimit": 10000,
      "currentUsage": 1250
    }
  ],
  "allFeatures": [
    // ... all features with enabled status
  ],
  "planName": "Pro Plan",
  "subscriptionStatus": "ACTIVE",
  "subscriptionExpiresAt": "2024-02-15T10:30:00Z",
  "isTrialPeriod": false,
  "trialExpiresAt": null,
  "tenantId": "tenant-123"
}
```

### Enabled Features Only Response
```json
[
  {
    "code": "advanced_analytics",
    "name": "Advanced Analytics",
    "description": "Access to advanced reporting and analytics",
    "category": "ANALYTICS",
    "enabled": true,
    "usageLimit": null,
    "currentUsage": 0
  },
  {
    "code": "api_calls",
    "name": "API Calls",
    "description": "Monthly API call quota",
    "category": "API",
    "enabled": true,
    "usageLimit": 10000,
    "currentUsage": 1250
  }
]
```