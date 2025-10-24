#!/bin/bash

# Docker deployment script for Bookstore Service
# Usage: ./scripts/docker-deploy.sh [environment]

set -e

ENVIRONMENT=${1:-local}

echo "Deploying Bookstore Service for environment: $ENVIRONMENT"

case $ENVIRONMENT in
  local)
    echo "Starting local development environment..."
    docker-compose -f docker-compose.yml up -d
    ;;
  staging)
    echo "Starting staging environment..."
    docker-compose -f docker-compose.staging.yml up -d
    ;;
  production)
    echo "Starting production environment..."
    docker-compose -f docker-compose.production.yml up -d
    ;;
  *)
    echo "Unknown environment: $ENVIRONMENT"
    echo "Supported environments: local, staging, production"
    exit 1
    ;;
esac

echo "Waiting for services to be healthy..."
sleep 30

# Check service health
echo "Checking service health..."
docker-compose -f docker-compose${ENVIRONMENT:+.$ENVIRONMENT}.yml ps

echo "Deployment completed for environment: $ENVIRONMENT"
echo "Service should be available at: http://localhost:8082"
echo "Health check: http://localhost:8082/actuator/health"
echo "API documentation: http://localhost:8082/swagger-ui.html"