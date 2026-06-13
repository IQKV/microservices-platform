# Helm Charts

> **Template reference only.** These charts are stored here as examples of the deployment patterns used by the platform. In the actual CI/CD flow, charts are maintained in a separate `HELM_CHARTS_REPOSITORY` and cloned by Drone at deploy time.

## Chart Overview

| Chart                                                      | Type           | Description                                         |
| ---------------------------------------------------------- | -------------- | --------------------------------------------------- |
| [`foundation-infra/`](foundation-infra/README.md)          | Infrastructure | PostgreSQL, Redis, RabbitMQ, MinIO, MailHog, DbGate |
| [`foundation-iam-service/`](#backend-service-charts)       | Backend        | IAM microservice (auth, multi-tenancy)              |
| [`foundation-gateway-service/`](#backend-service-charts)   | Backend        | API Gateway (Spring Cloud Gateway / WebFlux)        |
| [`foundation-billing-service/`](#backend-service-charts)   | Backend        | Billing microservice (Stripe subscriptions)         |
| [`foundation-audit-service/`](#backend-service-charts)     | Backend        | Audit microservice (event-driven log ingestion)     |
| [`foundation-ui-app/`](foundation-ui-app/README.md)        | Frontend       | Tenant App React SPA (Nginx container)              |
| [`foundation-ui-platform-admin/`](#frontend-charts)        | Frontend       | Platform Admin React SPA (Nginx container)          |
| [`foundation-ui-saas-landing-kit/`](#frontend-charts)      | Frontend       | SaaS Landing Kit (Astro/Nginx container)            |
| [`standard-ui-blank-astro-daisyui-ssr/`](#frontend-charts) | Frontend       | Blank Astro + DaisyUI SSR template                  |

## Structure Convention

Every chart follows the same layout:

```
{chart-name}/
├── Chart.yaml          # Chart metadata
├── values.yaml         # Base defaults (SIT environment)
├── values-sit.yaml     # SIT overrides
├── values-uat.yaml     # UAT overrides
├── values-prd.yaml     # Production overrides
└── templates/
    ├── _helpers.tpl
    ├── deployment.yaml
    ├── service.yaml
    ├── serviceaccount.yaml
    ├── configmap.yaml
    ├── ingress.yaml
    ├── hpa.yaml
    ├── networkpolicy.yaml
    ├── prometheusrule.yaml
    ├── servicemonitor.yaml
    └── NOTES.txt
```

Backend charts also include `secret.yaml` (except Gateway). Frontend charts include `poddisruptionbudget.yaml` instead.

## Backend Service Charts

All four backend charts share the same structure and common patterns:

### Common values

```yaml
image:
  repository: know-how.download/iqkv/{service-name}
  pullPolicy: Always

imagePullSecrets:
  - name: know-how-download-auth

securityContext:
  runAsNonRoot: true
  runAsUser: 1001
  capabilities:
    drop: [ALL]

healthcheck:
  livenessProbe:
    httpGet: { path: /actuator/health/liveness, port: 8081 }
  readinessProbe:
    httpGet: { path: /actuator/health/readiness, port: 8081 }

monitoring:
  serviceMonitor:
    enabled: false # enable when Prometheus Operator is present
  prometheusRule:
    enabled: false
```

### Management services

Each backend chart exposes two additional Kubernetes Services for the Spring Boot Actuator (port 8081):

| Service                 | Port    | Purpose                                                        |
| ----------------------- | ------- | -------------------------------------------------------------- |
| `managementService`     | 8081    | Direct Actuator access (health checks, metrics)                |
| `managementHttpService` | 80→8081 | HTTP URL without port number (e.g. for Gateway's JWKS polling) |

### JVM tuning

```yaml
jvm:
  maxRAMPercentage: "70.0"
  gcAlgorithm: "G1GC"
  maxGCPauseMillis: "200"
  additionalOpts: ""
```

Gateway additionally exposes Reactor Netty settings:

```yaml
jvm:
  reactorNettyIoWorkerCount: "4"
  reactorNettyMaxConnections: "500"
```

### Platform rollout mode

A `platform.rolloutMode` value must be identical across `iam-service`, `billing-service`, and `gateway-service`:

```yaml
platform:
  rolloutMode: "MULTI_TENANT" # or SINGLE_TENANT
  defaultTenantKey: "" # required for SINGLE_TENANT mode
```

### Per-service differences

| Chart                        | Notable values                                                                                                                               |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `foundation-iam-service`     | `infraServices.objectstorage.*` (MinIO), `config.jwt.*`, `config.mail.*`, `config.appBaseUrl`                                                |
| `foundation-gateway-service` | `config.iamServiceUri`, `config.billingServiceUri`, `config.auditServiceUri`, `config.iamManagementServiceUri`, `config.cors.allowedOrigins` |
| `foundation-billing-service` | `infraServices.postgresql.*`, `infraServices.rabbitmq.*`, Stripe secrets via `config.*`                                                      |
| `foundation-audit-service`   | `infraServices.postgresql.*`, `infraServices.rabbitmq.*`                                                                                     |

## Infrastructure Chart

See [`foundation-infra/README.md`](foundation-infra/README.md) for full documentation.

Deploys the following components into a single Helm release:

| Component     | Enabled by default | Notes                                                                   |
| ------------- | ------------------ | ----------------------------------------------------------------------- |
| PostgreSQL 17 | ✅                 | Runs DB init Job to create per-service databases and users              |
| Redis 7       | ✅                 | Auth enabled                                                            |
| RabbitMQ 3    | ✅                 | Topic exchange `iqkv.events` configured post-deploy                     |
| MinIO         | ✅                 | S3-compatible; creates `iqkv-iam-assets`, `iqkv-billing-assets` buckets |
| MailHog       | ✅                 | Local SMTP + web UI for email testing                                   |
| DbGate        | ❌                 | Optional web-based DB admin (PostgreSQL, Redis, RabbitMQ, MinIO)        |
| Nginx Ingress | ❌                 | Optional; exposes MailHog, RabbitMQ management, MinIO Console           |

Environment value files:

| File                      | Use case                           |
| ------------------------- | ---------------------------------- |
| `values.yaml`             | Base / SIT defaults                |
| `values-sit.yaml`         | SIT-specific overrides             |
| `values-k3s-full.yaml`    | K3s dev cluster with all services  |
| `values-k3s-minimal.yaml` | K3s dev cluster, minimal resources |

## Frontend Charts

Frontend charts are lighter than backend charts:

- No `secret.yaml` — no credentials managed by the chart
- Include `poddisruptionbudget.yaml` for production availability
- Use Nginx as the runtime (images built on `nginx-runner` base)
- Runtime configuration injected via ConfigMap + init container into `public/config.js`

### Runtime config injection pattern

```
Init container  →  copies config.js from ConfigMap  →  shared emptyDir volume
Main container  ←  Nginx serves static files + injected config.js
```

This avoids rebuilding the image per environment.

## Deploying

### Infrastructure

```bash
# SIT
helm upgrade --install foundation-infra ./foundation-infra \
  --values ./foundation-infra/values.yaml \
  --values ./foundation-infra/values-sit.yaml \
  --set config.postgresql.password=<PG_PASS> \
  --set config.rabbitmq.password=<RMQ_PASS> \
  --set config.redis.password=<REDIS_PASS> \
  --set config.objectstorage.accessKey=<S3_KEY> \
  --set config.objectstorage.secretKey=<S3_SECRET> \
  --namespace iqkv-sit-env --create-namespace
```

### Backend service

```bash
helm upgrade --install foundation-iam-service ./foundation-iam-service \
  --values ./foundation-iam-service/values.yaml \
  --values ./foundation-iam-service/values-sit.yaml \
  --set image.tag=<VERSION> \
  --set infraServices.postgresql.password=<PG_PASS> \
  --set infraServices.rabbitmq.password=<RMQ_PASS> \
  --set config.mail.username=<SMTP_USER> \
  --set config.mail.password=<SMTP_PASS> \
  --namespace iqkv-sit-env
```

### Frontend

```bash
helm upgrade --install foundation-ui-app ./foundation-ui-app \
  --values ./foundation-ui-app/values.yaml \
  --values ./foundation-ui-app/values-sit.yaml \
  --set image.tag=<VERSION> \
  --namespace iqkv-sit-env
```

## Port-forwarding for local access

```bash
kubectl port-forward svc/foundation-infra-postgresql 5432:5432 -n iqkv-sit-env
kubectl port-forward svc/foundation-infra-redis-master 6379:6379 -n iqkv-sit-env
kubectl port-forward svc/foundation-infra-rabbitmq 15672:15672 -n iqkv-sit-env
kubectl port-forward svc/foundation-infra-minio 9000:9000 -n iqkv-sit-env
kubectl port-forward svc/foundation-infra-mailhog 8025:8025 -n iqkv-sit-env
```
