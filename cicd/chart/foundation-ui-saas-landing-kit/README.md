# IQ Key Value SaaS Landing Kit - Helm Chart

A Helm chart for deploying a static SaaS landing page built with Astro, React, Tailwind CSS, and DaisyUI, served by Nginx.

## Overview

This chart deploys a containerized static website built with Astro and served by Nginx. The landing page provides a modern, performant marketing site for SaaS products.

## Features

- **Astro** static site generator with React islands
- **Tailwind CSS** and **DaisyUI** for styling
- **Pre-configured Nginx base image** with security headers
- **Customizable Nginx configuration** via mounted ConfigMap files
- **Security headers** and Content Security Policy configuration
- **Health checks** and monitoring support
- **Horizontal Pod Autoscaling** (HPA)
- **Lightweight** - minimal resource requirements

## Prerequisites

- Kubernetes 1.19+
- Helm 3.8+
- Nginx Ingress Controller (if ingress is enabled)

## Installation

### Quick Start (SIT)

```bash
# Install with development values
helm install landing-kit ./foundation-ui-saas-landing-kit \
  --namespace iqkv-sit-env \
  --create-namespace \
  --values values-sit.yaml
```

## Nginx Configuration

The chart uses a pre-configured Nginx base image (`cicdtools/nginx-runner`) with optimized defaults for static sites.

### Base Image Defaults

- **Default config**: Static file serving with gzip compression and caching
- **Security headers**: Basic security headers included
- **CSP headers**: Default Content Security Policy

### Configuration Overrides

```yaml
app:
  nginx:
    customConfig:
      # Add health check endpoint
      siteConf: |
        location /health {
            return 200 "healthy\n";
        }

      # Custom CSP for landing page
      contentSecurityPolicyConf: |
        add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline';" always;
```

## Configuration

### Key Configuration Sections

#### Image Configuration

```yaml
image:
  repository: iqkv/foundation-ui-saas-landing-kit
  tag: "latest"
  pullPolicy: Always

imagePullSecrets:
  - name: know-how-download-auth
```

#### Ingress Configuration

```yaml
ingress:
  enabled: true
  className: "nginx"
  hosts:
    - host: landing.iqkv.site
      paths:
        - path: /
          pathType: Prefix
```

#### Resource Configuration

```yaml
resources:
  limits:
    memory: 128Mi
    cpu: 100m
  requests:
    memory: 64Mi
    cpu: 50m
```

## Deployment Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Ingress       │    │   Service        │    │   Deployment    │
│   (nginx)       │───▶│   (ClusterIP)    │───▶│   (nginx +      │
│   Port 80/443   │    │   Port 80        │    │    static site) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Monitoring

### Health Checks

- **Liveness Probe**: HTTP GET `/` (ensures Nginx is responding)
- **Readiness Probe**: HTTP GET `/` (ensures app is ready to serve traffic)

### Prometheus Integration

```yaml
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
```

## Security

### Pod Security

- Runs as non-root user (UID 1001)
- Drops all capabilities
- Security context with seccomp profile

### Network Security

- Security headers (HSTS, X-Frame-Options, X-Content-Type-Options, etc.)
- Content Security Policy for XSS protection

## Troubleshooting

### Common Issues

1. **Pod not starting**

   ```bash
   kubectl describe pod -l app.kubernetes.io/name=foundation-ui-saas-landing-kit
   kubectl logs -l app.kubernetes.io/name=foundation-ui-saas-landing-kit
   ```

2. **Ingress not working**
   ```bash
   kubectl describe ingress foundation-ui-saas-landing-kit
   kubectl get events --sort-by=.metadata.creationTimestamp
   ```

### Debugging Commands

```bash
# Check deployment status
kubectl get deployment foundation-ui-saas-landing-kit

# Check pod logs
kubectl logs -l app.kubernetes.io/name=foundation-ui-saas-landing-kit -f

# Port forward for local testing
kubectl port-forward svc/foundation-ui-saas-landing-kit 8080:80
```

## Upgrading

```bash
# Upgrade to new version
helm upgrade landing-kit ./foundation-ui-saas-landing-kit \
  --namespace iqkv-sit-env \
  --values values-sit.yaml \
  --set image.tag="v1.2.0"

# Rollback if needed
helm rollback landing-kit 1 --namespace iqkv-sit-env
```

## Uninstallation

```bash
helm uninstall landing-kit --namespace iqkv-sit-env
```

## License

This Helm chart is licensed under the Apache-2.0 License.
