# Gripday Platform - Minikube Architecture

## Overview

The minikube deployment mirrors the production architecture (API Gateway pattern) while keeping additional debugging options available for local development.

## Architecture Comparison

### Production/Staging Architecture

```
Internet → Ingress → Gateway Service → Backend Services
                                        (auth, bookstore)
                                        NO direct access
```

### Minikube Architecture

```
Developer → Multiple Access Methods:

            1. Ingress (production-like)
               → Gateway → Backend Services

            2. NodePort (convenience)
               → Direct to any service

            3. Port Forward (debugging)
               → Direct to any pod
```

## Why Multiple Access Methods?

### 1. **Ingress (Production-like)** ✅ Recommended for Testing

- **URL**: `http://api.gripday.site`
- **Purpose**: Test the actual production flow
- **Requires**: Ingress addon, /etc/hosts configuration
- **Benefit**: Validates the API Gateway pattern

### 2. **NodePort (Direct Access)** 🔧 Quick Testing

- **URL**: `http://$(minikube ip):30080`
- **Purpose**: Quick API testing without DNS setup
- **Requires**: Nothing
- **Benefit**: Works immediately after deployment

### 3. **Direct Service Ingress** 🐛 Debugging Only

- **URL**: `http://auth.gripday.site`
- **Purpose**: Debug backend services directly
- **Requires**: Ingress addon, /etc/hosts
- **Note**: Does NOT exist in production!

## Deployment Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Developer Machine                         │
└────────┬─────────────────────┬──────────────────────────────┘
         │                     │
         │ /etc/hosts          │ Direct IP:Port
         │ DNS resolution      │ access
         │                     │
         ▼                     ▼
┌──────────────────┐   ┌──────────────────┐
│  Ingress        │   │   NodePort       │
│  (nginx)        │   │   Services       │
└────────┬─────────┘   └────────┬─────────┘
         │                      │
         └──────────┬───────────┘
                    │
         ┌──────────┼──────────┐
         │          │          │
         ▼          ▼          ▼
    ┌────────┐ ┌────────┐ ┌─────────┐
    │Gateway │ │  Auth  │ │Bookstore│
    │  8080  │ │  8080  │ │  8080   │
    └────┬───┘ └────────┘ └─────────┘
         │
         │ Internal K8s Network
         │
    ┌────┴─────┐
    │          │
    ▼          ▼
 ┌─────┐   ┌─────┐
 │Auth │   │Book │
 │8080 │   │8080 │
 └─────┘   └─────┘
```

## Access Methods Detail

### Method 1: Ingress (Production-like)

**Setup:**

```bash
# Enable ingress addon
minikube addons enable ingress

# Get minikube IP
minikube ip

# Add to /etc/hosts (Linux/Mac)
sudo nano /etc/hosts
# Add line:
192.168.49.2 api.gripday.site auth.gripday.site bookstore.gripday.site

# Or Windows: C:\Windows\System32\drivers\etc\hosts
```

**Usage:**

```bash
# Via Gateway (production-like)
curl http://api.gripday.site/api/v1/auth/login

# Direct to Auth (debugging only)
curl http://auth.gripday.site/api/v1/auth/login

# Direct to Bookstore (debugging only)
curl http://bookstore.gripday.site/api/v1/bookstore/books
```

**Benefits:**

- ✅ Tests production flow
- ✅ Human-readable URLs
- ✅ CORS configuration testing
- ✅ Validates gateway routing

**Drawbacks:**

- ⚠️ Requires DNS setup
- ⚠️ Requires ingress addon

---

### Method 2: NodePort (Quick Testing)

**Setup:**

```bash
# Nothing needed - works out of the box
```

**Usage:**

```bash
# Get minikube IP
MINIKUBE_IP=$(minikube ip)

# Gateway Service
curl http://$MINIKUBE_IP:30080/api/v1/auth/login

# User Service (direct)
curl http://$MINIKUBE_IP:30081/api/v1/auth/login

# Bookstore Service (direct)
curl http://$MINIKUBE_IP:30082/api/v1/bookstore/books
```

**Benefits:**

- ✅ Works immediately
- ✅ No DNS configuration
- ✅ Easy to script

**Drawbacks:**

- ⚠️ Not production-like
- ⚠️ IP-based (harder to remember)

---

### Method 3: Port Forward (Pod Debugging)

**Setup:**

```bash
# Forward gateway service
kubectl port-forward -n gripday svc/gateway-service 8080:8080

# Forward user service
kubectl port-forward -n gripday svc/user-service 8080:8080
```

**Usage:**

```bash
# Access on localhost
curl http://localhost:8080/api/v1/auth/login
curl http://localhost:8080/api/v1/auth/login
```

**Benefits:**

- ✅ Perfect for debugging
- ✅ Uses localhost
- ✅ Can forward to specific pods

**Drawbacks:**

- ⚠️ Blocks terminal
- ⚠️ Manual setup per service

---

## Recommended Workflow

### Day-to-Day Development

```bash
# Use NodePort for quick API testing
curl http://$(minikube ip):30080/api/v1/auth/login
```

### Testing API Gateway Logic

```bash
# Use Ingress to test production flow
curl http://api.gripday.site/api/v1/auth/login
```

### Debugging Backend Service

```bash
# Use direct service ingress OR port forward
curl http://auth.gripday.site/actuator/health
# OR
kubectl port-forward -n gripday svc/user-service 8080:8080
curl http://localhost:8080/actuator/health
```

## Configuration Files

### 1. `all-in-one.yaml`

- Single namespace: `gripday`
- All services (gateway, auth, bookstore)
- Databases (postgres, redis)
- NodePort services for direct access

### 2. `ingress.yaml`

- Gateway ingress (production-like)
- Auth ingress (debugging only)
- Bookstore ingress (debugging only)
- Optional - only applied if ingress addon enabled

### 3. `infrastructure-only.yaml`

- Just databases and Redis
- Useful for testing services locally with kubectl

## Key Differences from Production

| Feature                 | Minikube    | Production  | Reason                    |
| ----------------------- | ----------- | ----------- | ------------------------- |
| Backend Service Ingress | ✅ Yes      | ❌ No       | Debugging convenience     |
| NodePort Services       | ✅ Yes      | ❌ No       | Direct access without DNS |
| TLS/HTTPS               | ❌ No       | ✅ Yes      | Not needed for local      |
| Network Policies        | ⚠️ Optional | ✅ Enforced | Permissive for debugging  |
| Separate Namespaces     | ❌ No       | ✅ Yes      | Simpler for local         |
| Rate Limiting           | ⚠️ Lenient  | ✅ Strict   | Allow rapid testing       |
| Resource Limits         | ⚠️ Lower    | ✅ Higher   | Limited local resources   |

## Network Policy (Optional)

For testing production-like network isolation:

```bash
# Apply network policies to minikube
kubectl apply -f ../user-service/network-policy.yaml
kubectl apply -f ../gateway-service/network-policy.yaml
kubectl apply -f ../bookstore-service/network-policy.yaml

# Now only gateway can access backend services
# Direct NodePort/Ingress to auth/bookstore will be blocked
```

**Note**: This will disable debugging access methods!

## Troubleshooting

### Ingress Not Working

```bash
# Check if ingress addon is enabled
minikube addons list | grep ingress

# Enable if needed
minikube addons enable ingress

# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress resources
kubectl get ingress -n gripday

# Check /etc/hosts
cat /etc/hosts | grep gripday
```

### Services Not Accessible

```bash
# Check pods are running
kubectl get pods -n gripday

# Check services
kubectl get svc -n gripday

# Check logs
kubectl logs -n gripday deployment/gateway-service
kubectl logs -n gripday deployment/user-service

# Test internal connectivity
kubectl exec -n gripday deployment/gateway-service -- curl http://user-service:8080/actuator/health
```

### Gateway Not Routing

```bash
# Check gateway routes
curl http://$(minikube ip):30080/actuator/gateway/routes

# Check gateway logs
kubectl logs -n gripday -f deployment/gateway-service

# Test direct backend access
curl http://$(minikube ip):30081/actuator/health
```

## Quick Reference

| I want to...         | Use...                                                             |
| -------------------- | ------------------------------------------------------------------ |
| Quick API test       | NodePort: `http://$(minikube ip):30080`                            |
| Test production flow | Ingress: `http://api.gripday.site`                                 |
| Debug user service   | Direct: `http://auth.gripday.site` OR NodePort: `:30081`           |
| Debug bookstore      | Direct: `http://bookstore.gripday.site` OR NodePort: `:30082`      |
| Check gateway routes | `curl http://$(minikube ip):30080/actuator/gateway/routes`         |
| View logs            | `kubectl logs -n gripday deployment/<service-name>`                |
| Get into pod         | `kubectl exec -n gripday -it deployment/<service-name> -- /bin/sh` |

## Benefits of This Approach

1. ✅ **Production Parity**: Can test the actual API Gateway pattern
2. ✅ **Debugging Freedom**: Direct access when needed
3. ✅ **Gradual Migration**: Easy to move from direct access to gateway
4. ✅ **Learning**: Developers understand both patterns
5. ✅ **Flexibility**: Choose the right tool for the task

## Summary

Minikube provides **3 access methods** to balance production parity with development convenience:

1. **Ingress** → Production-like (preferred for integration testing)
2. **NodePort** → Quick testing (preferred for rapid development)
3. **Direct Service Ingress** → Debugging only (doesn't exist in production)

Use the appropriate method based on what you're testing or debugging!
