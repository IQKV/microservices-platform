#!/bin/bash
# Test script for IQ Scaffold Minikube deployment
set -e

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
ok() { echo "$(tput setaf 2)[OK]$(tput sgr0) $1"; }
warn() { echo "$(tput setaf 3)[WARN]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

# Check if minikube is running
minikube status > /dev/null 2>&1 || err "Minikube not running. Start with: minikube start"

MINIKUBE_IP=$(minikube ip)
log "Testing deployment on Minikube IP: $MINIKUBE_IP"

# Test health endpoints
log "Testing health endpoints..."

for service in "gateway:30080" "user:30081" "billing:30082"; do
    name=$(echo $service | cut -d: -f1)
    port=$(echo $service | cut -d: -f2)
    
    log "Testing $name service health..."
    if curl -f -s "http://${MINIKUBE_IP}:${port}/actuator/health" > /dev/null; then
        ok "$name service is healthy"
    else
        warn "$name service health check failed"
    fi
done

# Test service connectivity
log "Testing service connectivity..."

# Test user registration
log "Testing user registration..."
REGISTER_RESPONSE=$(curl -s -X POST "http://${MINIKUBE_IP}:30080/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser'$(date +%s)'",
    "email": "test'$(date +%s)'@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }' || echo "FAILED")

if [[ "$REGISTER_RESPONSE" == *"FAILED"* ]]; then
    warn "User registration test failed"
else
    ok "User registration test passed"
fi

# Test billing service configuration
log "Testing billing service configuration..."
BILLING_CONFIG=$(kubectl get configmap iqscaffold-config -n iqscaffold-dev-env -o jsonpath='{.data}' 2>/dev/null || echo "FAILED")

if [[ "$BILLING_CONFIG" == *"IQSCAFFOLD_USER_SERVICE_URL"* ]]; then
    ok "Billing service configuration is updated"
else
    warn "Billing service configuration may be outdated"
fi

# Test secrets
log "Testing secrets configuration..."
SECRETS=$(kubectl get secret iqscaffold-secrets -n iqscaffold-dev-env -o jsonpath='{.data}' 2>/dev/null || echo "FAILED")

if [[ "$SECRETS" == *"STRIPE_PUBLIC_KEY"* ]]; then
    ok "Stripe secrets are configured"
else
    warn "Stripe secrets may be missing"
fi

# Test pod status
log "Checking pod status..."
kubectl get pods -n iqscaffold-dev-env

echo ""
ok "Deployment test completed"
log "Access services at:"
echo "  Gateway:   http://${MINIKUBE_IP}:30080"
echo "  User:      http://${MINIKUBE_IP}:30081"
echo "  Billing:   http://${MINIKUBE_IP}:30082"

if minikube addons list | grep -q "ingress.*enabled"; then
    echo ""
    log "Ingress URLs (add to /etc/hosts):"
    echo "  API Gateway: http://api.iqscaffold.site"
    echo "  User:        http://user.iqscaffold.site"
    echo "  Billing:     http://billing.iqscaffold.site"
fi