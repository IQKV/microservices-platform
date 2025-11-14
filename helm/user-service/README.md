# User Service Helm Chart

This Helm chart deploys the Gripday User Service along with its dependencies (PostgreSQL and Redis) on a Kubernetes cluster.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- PV provisioner support in the underlying infrastructure
- Ingress controller (nginx) if ingress is enabled

## Installing the Chart

To install the chart with the release name `user-service`:

```bash
helm install user-service ./user-service
```

To install in a specific namespace:

```bash
helm install user-service ./user-service --namespace gripday-user --create-namespace
```

## Uninstalling the Chart

To uninstall/delete the `user-service` deployment:

```bash
helm uninstall user-service --namespace gripday-user
```

## Configuration

The following table lists the configurable parameters of the User Service chart and their default values.

### Global Parameters

| Parameter            | Description      | Default            |
| -------------------- | ---------------- | ------------------ |
| `global.environment` | Environment name | `local`            |
| `global.platform`    | Platform name    | `gripday` |

### Application Parameters

| Parameter          | Description        | Default                |
| ------------------ | ------------------ | ---------------------- |
| `replicaCount`     | Number of replicas | `2`                    |
| `image.repository` | Image repository   | `gripday/user-service` |
| `image.tag`        | Image tag          | `1.0.0`                |
| `image.pullPolicy` | Image pull policy  | `IfNotPresent`         |

### Service Parameters

| Parameter                  | Description             | Default     |
| -------------------------- | ----------------------- | ----------- |
| `service.type`             | Service type            | `ClusterIP` |
| `service.port`             | Service port            | `8080`      |
| `service.headless.enabled` | Create headless service | `true`      |

### Ingress Parameters

| Parameter               | Description        | Default             |
| ----------------------- | ------------------ | ------------------- |
| `ingress.enabled`       | Enable ingress     | `true`              |
| `ingress.className`     | Ingress class name | `nginx`             |
| `ingress.hosts[0].host` | Hostname           | `auth.gripday.site` |

### Resources

| Parameter                   | Description    | Default |
| --------------------------- | -------------- | ------- |
| `resources.requests.memory` | Memory request | `384Mi` |
| `resources.requests.cpu`    | CPU request    | `250m`  |
| `resources.limits.memory`   | Memory limit   | `768Mi` |
| `resources.limits.cpu`      | CPU limit      | `500m`  |

### PostgreSQL Parameters

| Parameter                              | Description          | Default       |
| -------------------------------------- | -------------------- | ------------- |
| `postgresql.enabled`                   | Enable PostgreSQL    | `true`        |
| `postgresql.image.tag`                 | PostgreSQL image tag | `15.8-alpine` |
| `postgresql.persistence.size`          | PVC size             | `5Gi`         |
| `postgresql.resources.requests.memory` | Memory request       | `256Mi`       |
| `postgresql.resources.requests.cpu`    | CPU request          | `250m`        |

### Redis Parameters

| Parameter                | Description     | Default      |
| ------------------------ | --------------- | ------------ |
| `redis.enabled`          | Enable Redis    | `true`       |
| `redis.image.tag`        | Redis image tag | `7.2-alpine` |
| `redis.persistence.size` | PVC size        | `1Gi`        |
| `redis.config.maxmemory` | Max memory      | `256mb`      |

### Security Parameters

| Parameter                      | Description           | Default         |
| ------------------------------ | --------------------- | --------------- |
| `networkPolicy.enabled`        | Enable network policy | `true`          |
| `podSecurityContext.runAsUser` | Run as user ID        | `1001`          |
| `priorityClassName`            | Priority class name   | `high-priority` |

## Examples

### Install with custom values

```bash
helm install user-service ./user-service -f custom-values.yaml
```

### Upgrade with new values

```bash
helm upgrade user-service ./user-service --set replicaCount=3
```

### Install without PostgreSQL (use external database)

```bash
helm install user-service ./user-service --set postgresql.enabled=false
```

### Enable autoscaling

```bash
helm install user-service ./user-service \
  --set autoscaling.enabled=true \
  --set autoscaling.minReplicas=2 \
  --set autoscaling.maxReplicas=10
```

## Values Files for Different Environments

### Local Development (values.yaml)

Default values file

### Staging (values-staging.yaml)

```bash
helm install user-service ./user-service -f values-staging.yaml
```

### Production (values-production.yaml)

```bash
helm install user-service ./user-service -f values-production.yaml
```

## Troubleshooting

### Check pod status

```bash
kubectl get pods -n gripday-user
```

### View logs

```bash
kubectl logs -f -n gripday-user -l app.kubernetes.io/name=gripday-user-service
```

### Check service endpoints

```bash
kubectl get endpoints -n gripday-user
```

### Describe pod for events

```bash
kubectl describe pod <pod-name> -n gripday-user
```

## Support

For issues and questions, please contact the Gripday Platform Team.
