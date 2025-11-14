# Kubernetes Secrets Management Guide

## ⚠️ CRITICAL SECURITY WARNING

**DO NOT commit base64-encoded secrets to Git!** Even though they appear encoded, base64 is trivially reversible and provides NO security.

## Current State

The current `secret.yaml` files contain base64-encoded placeholder values. These are **ONLY for local development** and must be replaced in staging/production environments.

## Recommended Approaches

### 1. External Secrets Operator (Recommended for Production)

Use [External Secrets Operator](https://external-secrets.io/) with a secrets manager:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: user-service-secrets
  namespace: gripday-dev-env
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: user-service-secrets
    creationPolicy: Owner
  data:
    - secretKey: GRIPDAY_DATABASE_PASSWORD
      remoteRef:
        key: gripday/auth/database
        property: password
    - secretKey: GRIPDAY_AUTH_JWT_SECRET
      remoteRef:
        key: gripday/auth/jwt
        property: secret
```

**Supported Backends:**

- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- Google Cloud Secret Manager
- 1Password

### 2. Sealed Secrets (GitOps-friendly)

Use [Bitnami Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) for encrypting secrets that can be stored in Git:

```bash
# Install kubeseal CLI
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create a sealed secret
kubectl create secret generic user-service-secrets \
  --from-literal=GRIPDAY_DATABASE_PASSWORD=mypassword \
  --dry-run=client -o yaml | \
  kubeseal -o yaml > user-service-sealed-secret.yaml

# Commit the sealed secret to Git (safe!)
git add user-service-sealed-secret.yaml
```

### 3. Kubernetes Native Secrets with RBAC

For simpler deployments, use native Kubernetes secrets with strict RBAC:

```bash
# Create secret from file (never commit the file)
kubectl create secret generic user-service-secrets \
  --from-env-file=.env.production \
  --namespace=gripday-user

# Or from literals
kubectl create secret generic user-service-secrets \
  --from-literal=GRIPDAY_DATABASE_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=GRIPDAY_AUTH_JWT_SECRET=$(openssl rand -base64 64) \
  --namespace=gripday-user
```

### 4. SOPS (Secrets OPerationS)

Use [SOPS](https://github.com/mozilla/sops) for encrypting YAML files:

```bash
# Install SOPS
brew install sops  # macOS
# or download from releases

# Encrypt a secret file
sops --encrypt --age <your-age-key> secret.yaml > secret.enc.yaml

# Decrypt and apply
sops --decrypt secret.enc.yaml | kubectl apply -f -
```

## Best Practices

### 1. Secret Rotation

Implement regular secret rotation:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: rotate-secrets
  namespace: gripday-dev-env
spec:
  schedule: "0 0 1 * *" # Monthly
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: rotate
              image: your-rotation-script:latest
              command: ["/scripts/rotate-secrets.sh"]
```

### 2. Audit Logging

Enable audit logging for secret access:

```yaml
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
  - level: RequestResponse
    resources:
      - group: ""
        resources: ["secrets"]
    namespaces: ["gripday-user", "gripday-bookstore", "gripday-gateway"]
```

### 3. RBAC Restrictions

Limit who can read secrets:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
  namespace: gripday-dev-env
rules:
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: ["user-service-secrets"]
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: user-service-secret-reader
  namespace: gripday-dev-env
subjects:
  - kind: ServiceAccount
    name: user-service-sa
    namespace: gripday-dev-env
roleRef:
  kind: Role
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

### 4. Encryption at Rest

Enable encryption at rest for etcd:

```yaml
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
  - resources:
      - secrets
    providers:
      - aescbc:
          keys:
            - name: key1
              secret: <base64-encoded-32-byte-key>
      - identity: {}
```

## Migration Steps

### From Current Setup to External Secrets Operator

1. **Install External Secrets Operator:**

   ```bash
   helm repo add external-secrets https://charts.external-secrets.io
   helm install external-secrets external-secrets/external-secrets \
     -n external-secrets-system --create-namespace
   ```

2. **Set up your secrets backend (e.g., HashiCorp Vault):**

   ```bash
   vault kv put secret/gripday/auth/database password="secure-password"
   vault kv put secret/gripday/auth/jwt secret="secure-jwt-secret"
   ```

3. **Create SecretStore:**

   ```yaml
   apiVersion: external-secrets.io/v1beta1
   kind: SecretStore
   metadata:
     name: vault-backend
     namespace: gripday-dev-env
   spec:
     provider:
       vault:
         server: "https://vault.example.com"
         path: "secret"
         version: "v2"
         auth:
           kubernetes:
             mountPath: "kubernetes"
             role: "gripday-user"
   ```

4. **Create ExternalSecret resources** (see example above)

5. **Delete old Secret manifests from Git:**
   ```bash
   git rm k8s/*/secret.yaml
   git commit -m "Remove hardcoded secrets, using External Secrets Operator"
   ```

## Environment-Specific Secrets

### Local Development

Use `.env` files that are gitignored:

```bash
# .env.local (NEVER commit this)
GRIPDAY_DATABASE_PASSWORD=localpass
GRIPDAY_AUTH_JWT_SECRET=local-jwt-secret
```

### Staging/Production

Use secrets manager or sealed secrets. Never use the same secrets across environments.

## Generating Strong Secrets

```bash
# For passwords (32 characters)
openssl rand -base64 32

# For JWT secrets (64 characters)
openssl rand -base64 64

# For API keys (hex format)
openssl rand -hex 32
```

## Monitoring and Alerts

Set up alerts for:

- Unauthorized secret access attempts
- Secret rotation failures
- Expired secrets
- Secrets accessed by unexpected pods

## Checklist

- [ ] Remove `secret.yaml` files from Git
- [ ] Set up External Secrets Operator or Sealed Secrets
- [ ] Configure secrets backend (Vault, AWS SM, etc.)
- [ ] Implement secret rotation
- [ ] Enable encryption at rest
- [ ] Set up RBAC for secret access
- [ ] Configure audit logging
- [ ] Document secret recovery process
- [ ] Test secret rotation in staging
- [ ] Monitor secret access patterns

## References

- [Kubernetes Secrets Best Practices](https://kubernetes.io/docs/concepts/security/secrets-good-practices/)
- [External Secrets Operator](https://external-secrets.io/)
- [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)
- [SOPS](https://github.com/mozilla/sops)
- [HashiCorp Vault](https://www.vaultproject.io/)
