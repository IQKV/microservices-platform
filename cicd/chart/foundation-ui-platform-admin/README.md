# IQ Key Value Admin Dashboard UI Helm Chart

A Helm chart for deploying the IQ Key Value Application Portal UI - a modern React application with Nginx serving, runtime configuration via ConfigMap, and SPA routing support.

## Description

This chart deploys a React-based application portal UI that provides:

- **Dashboard Analytics** - KPI cards with trend indicators and real-time stats
- **User & Security Management** - User administration with role-based access control
- **Payment & Billing System** - Gateway support with Stripe integration
- **Multi-Tenant Support** - Tenant context propagation and isolation patterns

## Architecture

The application uses:

- **React 19** with TypeScript and Feature-Sliced Design architecture
- **Vite 8** for fast development and optimized builds
- **Mantine UI** for modern React components
- **TanStack Router & Query** for routing and data management
- **Nginx** serving static files with runtime configuration injection

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- PV provisioner support in the underlying infrastructure (if persistence is enabled)

## Installing the Chart

To install the chart with the release name `admin-dashboard`:

```bash
# Development environment
helm install admin-dashboard ./foundation-ui-platform-admin -f values-sit.yaml

# Staging environment
helm install admin-dashboard ./foundation-ui-platform-admin -f values-uat.yaml

# Production environment
helm install admin-dashboard ./foundation-ui-platform-admin -f values-prd.yaml
```

## Uninstalling the Chart

To uninstall/delete the `admin-dashboard` deployment:

```bash
helm delete admin-dashboard
```

## Configuration

The following table lists the configurable parameters of the chart and their default values.

### Application Configuration

| Parameter         | Description         | Default                        |
| ----------------- | ------------------- | ------------------------------ |
| `app.name`        | Application name    | `foundation-ui-platform-admin` |
| `app.version`     | Application version | `1.0.0`                        |
| `app.environment` | Environment name    | `dev`                          |
| `app.nginx.port`  | Nginx port          | `8080`                         |

### Runtime Environment Variables

| Parameter              | Description                | Default                 |
| ---------------------- | -------------------------- | ----------------------- |
| `app.env.apiServerUrl` | Backend API URL            | `http://localhost:8080` |
| `app.env.enableMsw`    | Enable Mock Service Worker | `false`                 |
| `app.env.logLevel`     | Logging level              | `info`                  |

### Image Configuration

| Parameter          | Description        | Default                             |
| ------------------ | ------------------ | ----------------------------------- |
| `image.repository` | Image repository   | `iqkv/foundation-ui-platform-admin` |
| `image.tag`        | Image tag          | `latest`                            |
| `image.pullPolicy` | Image pull policy  | `Always`                            |
| `imagePullSecrets` | Image pull secrets | `[{name: know-how-download-auth}]`  |

### Deployment Configuration

| Parameter                   | Description        | Default |
| --------------------------- | ------------------ | ------- |
| `replicaCount`              | Number of replicas | `1`     |
| `resources.limits.memory`   | Memory limit       | `512Mi` |
| `resources.limits.cpu`      | CPU limit          | `500m`  |
| `resources.requests.memory` | Memory request     | `256Mi` |
| `resources.requests.cpu`    | CPU request        | `250m`  |

### Service Configuration

| Parameter            | Description  | Default     |
| -------------------- | ------------ | ----------- |
| `service.type`       | Service type | `ClusterIP` |
| `service.port`       | Service port | `80`        |
| `service.targetPort` | Target port  | `8080`      |

### Ingress Configuration

| Parameter                         | Description         | Default |
| --------------------------------- | ------------------- | ------- |
| `ingress.enabled`                 | Enable ingress      | `false` |
| `ingress.className`               | Ingress class name  | `""`    |
| `ingress.tls.enabled`             | Enable TLS          | `false` |
| `ingress.tls.certManager.enabled` | Enable cert-manager | `false` |
| `ingress.hosts`                   | Ingress hosts       | `[]`    |

### Autoscaling Configuration

| Parameter                                       | Description      | Default |
| ----------------------------------------------- | ---------------- | ------- |
| `autoscaling.enabled`                           | Enable HPA       | `false` |
| `autoscaling.minReplicas`                       | Minimum replicas | `1`     |
| `autoscaling.maxReplicas`                       | Maximum replicas | `100`   |
| `autoscaling.targetCPUUtilizationPercentage`    | CPU target       | `80`    |
| `autoscaling.targetMemoryUtilizationPercentage` | Memory target    | `80`    |

### Monitoring Configuration

| Parameter                           | Description           | Default |
| ----------------------------------- | --------------------- | ------- |
| `monitoring.enabled`                | Enable monitoring     | `false` |
| `monitoring.serviceMonitor.enabled` | Enable ServiceMonitor | `false` |
| `monitoring.prometheusRule.enabled` | Enable PrometheusRule | `false` |

## Environment-Specific Configurations

### Development (`values-sit.yaml`)

- Single replica
- Lower resource limits
- Debug logging
- No TLS
- No monitoring
- Development domains

### Staging (`values-uat.yaml`)

- 2 replicas with basic HPA
- Medium resource limits
- Info logging
- TLS with Let's Encrypt staging
- Basic monitoring
- Staging domains

### Production (`values-prd.yaml`)

- 3+ replicas with advanced HPA
- High resource limits
- Warn logging
- TLS with Let's Encrypt production
- Full monitoring with alerts
- Production domains
- Pod disruption budget
- Anti-affinity rules
- Enhanced security headers

## Runtime Configuration

The application uses runtime configuration injection via ConfigMap:

1. **Init Container**: Copies `config.js` from ConfigMap to shared volume
2. **Main Container**: Nginx serves static files + injected config
3. **Application**: Loads config at runtime (no rebuild needed)

This allows environment changes without rebuilding the Docker image.

## Security Features

- Non-root user (UID 1001)
- Dropped capabilities
- Security headers (HSTS, CSP, X-Frame-Options)
- CORS configuration
- Optional read-only root filesystem (production)

## Nginx Configuration

The chart uses a pre-configured Nginx base image with:

- Optimized defaults for React SPAs
- SPA routing support (fallback to index.html)
- Security headers
- Gzip compression

Custom configurations can be mounted when needed:

- `site.conf` - Additional location blocks
- `content-security-policy.conf` - Custom CSP headers
- `security.conf` - Additional security headers

## Monitoring

When monitoring is enabled, the chart provides:

### ServiceMonitor

- Scrapes metrics from `/metrics` endpoint
- Configurable interval and timeout
- Labels and annotations support

### PrometheusRule

- **AppPortalDown** - Service unavailable alert
- **AppPortalHighMemoryUsage** - Memory usage > 80%
- **AppPortalHighCPUUsage** - CPU usage > 80%
- **AppPortalPodCrashLooping** - Pod restart alerts

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -l "app.kubernetes.io/name=foundation-ui-platform-admin"
```

### View Logs

```bash
kubectl logs -l "app.kubernetes.io/name=foundation-ui-platform-admin"
```

### Check Configuration

```bash
kubectl get configmap <release-name>-foundation-ui-platform-admin-config -o yaml
```

### Port Forward for Local Access

```bash
kubectl port-forward svc/<release-name>-foundation-ui-platform-admin 8080:80
```

### Common Issues

1. **Pod not starting**: Check image pull secrets and repository access
2. **Configuration not loading**: Verify ConfigMap content and init container logs
3. **SPA routing not working**: Ensure Nginx configuration includes fallback to index.html
4. **CORS errors**: Check ingress annotations and security headers configuration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with different environments
5. Submit a pull request

## License

This chart is licensed under the MIT License.
