# Gateway Service Helm Chart

API Gateway for Gripday Platform using Spring Cloud Gateway.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- User Service and Bookstore Service deployed

## Installation

```bash
helm install gateway-service ./gateway-service --namespace gripday-gateway --create-namespace
```

## Configuration

Key parameters:

- `replicaCount`: 3
- `service.port`: 8080
- `autoscaling.enabled`: true
- `redis.enabled`: true

## Uninstall

```bash
helm uninstall gateway-service --namespace gripday-gateway
```

## Support

Contact: Gripday Platform Team
