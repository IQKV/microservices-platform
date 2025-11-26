#!/bin/bash
set -e

ENV=${1:-local}

[[ "$ENV" =~ ^(local|staging|production)$ ]] || { echo "Invalid environment: $ENV"; exit 1; }

echo "🚀 Deploying bookstore service ($ENV)..."

COMPOSE_FILE="docker-compose${ENV:+.$ENV}.yml"
[ "$ENV" = "local" ] && COMPOSE_FILE="docker-compose.yml"

docker-compose -f $COMPOSE_FILE up -d

echo "✅ Deployed: http://localhost:8080"