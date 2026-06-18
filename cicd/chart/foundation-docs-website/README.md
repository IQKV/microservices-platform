# IQ Key Value Documentation Website Helm Chart

A Helm chart for deploying the IQ Key Value Documentation Website - a VitePress-based documentation site with Nginx serving.

## Description

This chart deploys a VitePress-based documentation website that provides:

- **VitePress Static Site** - Fast, modern documentation framework
- **Nginx Serving** - Optimized static file serving with security headers
- **Environment-Specific Configs** - Separate values files for sit/uat/prd

## Architecture

The application uses:

- **VitePress** for static site generation
- **Nginx** for serving static files with optimized defaults
- **Helm** for Kubernetes deployment management

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+

## Installing the Chart

To install the chart with the release name `docs-website`:

```bash
# Development environment
helm install docs-website ./foundation-docs-website -f values-sit.yaml

# Staging environment
helm install docs-website ./foundation-docs-website -f values-uat.yaml

# Production environment
helm install docs-website ./foundation-docs-website -f values-prd.yaml
```

## Uninstalling the Chart

To uninstall/delete the `docs-website` deployment:

```bash
helm delete docs-website
```

## Configuration

The following table lists the configurable parameters of the chart and their default values.

### Application Configuration

| Parameter         | Description         | Default                            |
| ----------------- | ------------------- | ---------------------------------- |
| `app.name`        | Application name    | `foundation-docs-website`          |
| `app.version`     | Application version | `1.0.0`                            |
| `app.environment` | Environment name    | `sit`                              |
| `app.nginx.port`  | Nginx port          | `8080`                             |

### Image Configuration

| Parameter          | Description        | Default                                                         |
| ------------------ | ------------------ | --------------------------------------------------------------- |
| `image.repository` | Image repository   | `know-how.download/iqkv/foundation-docs-website`                |
| `image.tag`        | Image tag          | `latest`                                                        |
| `image.pullPolicy` | Image pull policy  | `Always`                                                        |
| `imagePullSecrets` | Image pull secrets | `[{name: know-how-download-auth}]`                              |

### Deployment Configuration

| Parameter                   | Description        | Default |
| --------------------------- | ------------------ | ------- |
| `replicaCount`              | Number of replicas | `1`     |
| `resources.limits.memory`   | Memory limit       | `256Mi` |
| `resources.limits.cpu`      | CPU limit          | `250m`  |
| `resources.requests.memory` | Memory request     | `128Mi` |
| `resources.requests.cpu`    | CPU request        | `100m`  |

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
- No TLS
- No monitoring
- Development domains

### Staging (`values-uat.yaml`)

- 2 replicas with basic HPA
- Medium resource limits
- TLS with Let's Encrypt staging
- Basic monitoring
- Staging domains

### Production (`values-prd.yaml`)

- 3+ replicas with advanced HPA
- High resource limits
- TLS with Let's Encrypt production
- Full monitoring with alerts
- Production domains
- Pod disruption budget
- Enhanced security headers

## Security Features

- Non-root user (UID 1001)
- Dropped capabilities
- Security headers (HSTS, CSP, X-Frame-Options)
- Optional read-only root filesystem

## Nginx Configuration

The chart uses a pre-configured Nginx base image with:

- Optimized defaults for static sites
- Gzip compression
- Security headers
- Content-Security-Policy (CSP)

Custom configurations can be mounted when needed:

- `site.conf` - Additional location blocks
- `content-security-policy.conf` - Custom CSP headers
- `security.conf` - Additional security headers

## Monitoring

When monitoring is enabled, the chart provides:

### ServiceMonitor

- Scrapes metrics from `/metrics` endpoint (if available)
- Configurable interval and timeout
- Labels and annotations support

### PrometheusRule

- **DocsDown** - Service unavailable alert
- **DocsHighMemoryUsage** - Memory usage > 80%
- **DocsHighCPUUsage** - CPU usage > 80%
- **DocsPodCrashLooping** - Pod restart alerts

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -l "app.kubernetes.io/name=foundation-docs-website"
```

### View Logs

```bash
kubectl logs -l "app.kubernetes.io/name=foundation-docs-website"
```

### Check Configuration

```bash
kubectl get configmap <release-name>-foundation-docs-website-config -o yaml
```

### Port Forward for Local Access

```bash
kubectl port-forward svc/<release-name>-foundation-docs-website 8080:80
```

### Common Issues

1. **Pod not starting**: Check image pull secrets and repository access
2. **404 errors**: Verify the static files are correctly built into the container
3. **CORS errors**: Check ingress annotations and security headers configuration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with different environments
5. Submit a pull request

## License

This chart is licensed under the MIT License.
