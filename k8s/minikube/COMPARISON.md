# Minikube vs Production K8s Manifests - Comparison

This document explains the differences between the minikube-optimized manifests and the production K8s manifests.

## Key Differences

### 1. Resource Requirements

**Minikube (Local Development)**
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**Production**
```yaml
resources:
  requests:
    memory: "384Mi"
    cpu: "250m"
  limits:
    memory: "768Mi"
    cpu: "500m"
```

**Why:** Minikube runs on limited laptop/desktop resources. Lower requests allow more pods to fit.

### 2. Replicas

| Service | Minikube | Production |
|---------|----------|------------|
| Gateway | 1 | 3 |
| Auth | 1 | 3 |
| Bookstore | 1 | 2 |

**Why:** Single replicas reduce resource usage for local development. Production needs redundancy.

### 3. Service Types

**Minikube:** `NodePort` (30080, 30081, 30082)
```yaml
spec:
  type: NodePort
  ports:
  - port: 8080
    nodePort: 30080
```

**Production:** `ClusterIP` with Ingress
```yaml
spec:
  type: ClusterIP
  ports:
  - port: 8080
```

**Why:** NodePort provides easy access in minikube. Production uses Ingress for proper routing and TLS.

### 4. Storage

**Minikube:** `emptyDir` (ephemeral)
```yaml
volumes:
- name: postgres-data
  emptyDir: {}
```

**Production:** `PersistentVolumeClaim`
```yaml
volumes:
- name: postgres-data
  persistentVolumeClaim:
    claimName: postgres-auth-pvc
```

**Why:** Data persistence not needed for local development. Production requires durable storage.

### 5. Security Context

**Minikube:** Relaxed
- No pod security policies
- Simplified service accounts
- Basic security context

**Production:** Hardened
- Pod security policies enforced
- Network policies enabled
- Read-only root filesystem
- Non-root user enforcement
- Capability dropping

**Why:** Local development prioritizes ease of use. Production needs defense in depth.

### 6. Namespaces

**Minikube:** Single namespace `gripday`
```yaml
namespace: gripday
```

**Production:** Environment-specific namespaces
```yaml
namespace: gripday-production-env
namespace: gripday-staging-env
```

**Why:** Simple namespace structure for local development. Production isolates by service and environment.

### 7. Configuration Management

**Minikube:** All-in-one file
- Single `all-in-one.yaml` contains everything
- Embedded configuration
- Default secrets included

**Production:** Separated by concern
- Separate files per resource type
- External secret management
- Environment-specific ConfigMaps

**Why:** One-command deployment for minikube. Production needs granular control and security.

### 8. Probes and Timeouts

**Minikube:** Longer timeouts
```yaml
startupProbe:
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 12  # 2 minutes
```

**Production:** Tighter SLAs
```yaml
startupProbe:
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 12
```

**Why:** Similar but minikube can be more lenient with slower startup on laptops.

### 9. Init Containers

**Minikube:** Simple wait logic
```yaml
initContainers:
- name: wait-for-postgres
  image: busybox:1.36
  command:
  - sh
  - -c
  - until nc -z postgres-auth 5432; do sleep 2; done
```

**Production:** Robust readiness checks
- Health endpoint verification
- Retry logic with exponential backoff
- Comprehensive error handling

**Why:** Minikube uses simple checks. Production needs reliability.

### 10. Observability

**Minikube:** Basic
- Health checks enabled
- Basic logging
- Optional metrics

**Production:** Comprehensive
- Prometheus metrics
- OpenTelemetry tracing
- Structured JSON logging
- Grafana dashboards
- Loki log aggregation

**Why:** Local development needs quick feedback. Production requires full observability.

### 11. Image Pull Policy

**Minikube:** `IfNotPresent`
```yaml
imagePullPolicy: IfNotPresent
```

**Production:** `Always` or specific tags
```yaml
imagePullPolicy: Always
image: gripday/auth-service:v1.2.3
```

**Why:** Minikube uses local images. Production pulls from registry.

### 12. Auto-scaling

**Minikube:** None
- Fixed single replica
- No HPA configured

**Production:** Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

**Why:** Fixed resources in minikube. Production scales with load.

### 13. Network Policies

**Minikube:** None
- All pods can communicate freely

**Production:** Restricted
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
spec:
  podSelector:
    matchLabels:
      app: auth-service
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: gateway-service
```

**Why:** Minikube prioritizes simplicity. Production enforces least privilege.

### 14. TLS/SSL

**Minikube:** None
- HTTP only
- No certificates

**Production:** Enforced
- HTTPS only
- Cert-manager integration
- Let's Encrypt certificates
- TLS termination at ingress

**Why:** Local development uses plain HTTP. Production requires encryption.

### 15. Pod Disruption Budgets

**Minikube:** None

**Production:** Configured
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
spec:
  minAvailable: 1
```

**Why:** Not needed for single replica. Production ensures availability during updates.

## When to Use Each

### Use Minikube Manifests When:

- ✅ Local development on laptop/desktop
- ✅ Quick testing and iteration
- ✅ Learning Kubernetes basics
- ✅ Running on limited resources
- ✅ Need fast deployment cycles
- ✅ Don't need data persistence
- ✅ Single developer environment

### Use Production Manifests When:

- ✅ Staging or production deployment
- ✅ Need high availability
- ✅ Require data persistence
- ✅ Need security hardening
- ✅ Multiple environments (dev/staging/prod)
- ✅ Team collaboration
- ✅ Compliance requirements
- ✅ Need monitoring and alerting

## Migration Path

### From Minikube to Production

1. **Build and push images** to a container registry
2. **Configure persistent storage** with PVCs
3. **Set up Ingress** with TLS certificates
4. **Enable security policies** and network policies
5. **Configure secrets management** (Vault, sealed-secrets)
6. **Set up monitoring** (Prometheus, Grafana)
7. **Configure auto-scaling** based on metrics
8. **Implement backup strategy** for databases
9. **Set up CI/CD pipelines** for automated deployment
10. **Configure DNS** and external access

### Quick Comparison Table

| Feature | Minikube | Production |
|---------|----------|------------|
| Deployment Time | < 5 min | 15-30 min |
| Resource Usage | Low | High |
| Complexity | Simple | Complex |
| Replicas | 1 | 2-10 |
| Storage | Ephemeral | Persistent |
| Networking | NodePort | Ingress + TLS |
| Security | Basic | Hardened |
| Observability | Basic | Full stack |
| Auto-scaling | No | Yes |
| Cost | Free | $$$ |

## Best Practices

### For Minikube Development:

1. Keep manifests simple and flat
2. Use single all-in-one file for easy deployment
3. Optimize for fast startup and teardown
4. Use NodePort for easy access
5. Don't worry about data persistence
6. Focus on functionality over security
7. Use local Docker images

### For Production:

1. Separate manifests by resource type
2. Use Kustomize or Helm for configuration
3. Implement security
4. Use external secrets management
5. Configure proper monitoring and alerting
6. Plan for disaster recovery
7. Use image tags, not `latest`
8. Implement network policies
9. Configure resource quotas
10. Use separate namespaces per environment

## Conclusion

The minikube manifests prioritize **developer experience** and **ease of use**, while production manifests prioritize **reliability**, **security**, and **scalability**. Both serve their purpose well in their respective environments.

Choose the right tool for your needs:
- **Developing?** Use minikube manifests
- **Deploying?** Use production manifests
