# IQ Key Value Infrastructure Services Helm Chart

This Helm chart deploys core infrastructure services for the IQ Key Value microservices platform, optimized for K3s development environments.

## 🎯 Key Features

- \*\*Multi-Tenant Schema Provisioning: Automatically creates `public, `t_platform`, `t_demo0001`, and `t_acme0001` schemas for all microservices
- \*\*Zero-Configuration Startup: Services start with all required schemas pre-provisioned
- \*\*No Race Conditions: Infrastructure-level schema creation eliminates event-driven timing issues

## Components

- **PostgreSQL 17**: Multi-database setup with configurable services
- **Redis 7**: Lightweight caching and session storage
- **RabbitMQ 3**: Message queue with management interface
- **MinIO**: S3-compatible object storage for file uploads and backups
- **MailHog**: Email testing service for development
- **DbGate**: Database administration tool (optional)
- **Nginx Ingress**: Web access to admin interfaces (optional)

## Prerequisites

- Kubernetes cluster (K3s recommended for development)
- Helm 3.2.0+
- Local-path storage provisioner (included in K3s)

## Quick Start

### Install with Default Configuration

```bash
# Install with default values
helm install foundation-infra ./foundation-infra

# Install with K3s minimal configuration
helm install foundation-infra ./foundation-infra -f values-k3s-minimal.yaml

# Install with K3s full configuration
helm install foundation-infra ./foundation-infra -f values-k3s-full.yaml

# Install with SIT (Staging) configuration
helm install foundation-infra ./foundation-infra -f values-sit.yaml
```

### Enable additional services:

```bash
# Install with DbGate database administration
helm install foundation-infra ./foundation-infra --set dbgate.enabled=true

# Install with MinIO object storage
helm install foundation-infra ./foundation-infra --set minio.enabled=true

# Install with web access via ingress
helm install foundation-infra ./foundation-infra --set ingress.enabled=true
```

## Configuration

For full configuration options, see `values.yaml`.

Key configuration sections:

- `config`: Shared configuration for services (environment, passwords, microservices list, etc.
- `postgresql`: PostgreSQL database configuration
- `redis`: Redis cache configuration
- `rabbitmq`: RabbitMQ message queue configuration
- `minio`: MinIO object storage configuration
- `mailhog`: MailHog email testing configuration
- `dbgate`: DbGate database administration configuration
- `ingress`: Ingress configuration
- `networkPolicy`: Network policies
- `dev`: Development helpers

## Service Access

After installation, services are available at:

- **PostgreSQL**: `<release-name>-postgresql:5432`
- **Redis**: `<release-name>-redis-master:6379`
- **RabbitMQ**: `<release-name>-rabbitmq:5672` (AMQP), `:15672` (Management)
- **MinIO**: `<release-name>-minio:9000` (API), `:9001` (Console)
- **MailHog**: `<release-name>-mailhog:1025` (SMTP), `:8025` (Web UI)
- **DbGate**: `<release-name>-dbgate:3000` (Web UI, if enabled)

## Access services from your local machine:

```bash
# Port forward to access services locally
kubectl port-forward svc/foundation-infra-postgresql 5432:5432
kubectl port-forward svc/foundation-infra-redis-master 6379:6379
kubectl port-forward svc/foundation-infra-rabbitmq 15672:15672
kubectl port-forward svc/foundation-infra-minio 9000:9000
kubectl port-forward svc/foundation-infra-minio 9001:9001
kubectl port-forward svc/foundation-infra-mailhog 8025:8025
kubectl port-forward svc/foundation-infra-dbgate 3000:3000  # If enabled
```

Get detailed connection information:

```bash
helm get notes foundation-infra
```
