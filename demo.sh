#!/bin/bash

# Platform Demo All-in-One Launcher
# This script starts the entire platform stack for demonstration purposes.

echo "Starting Platform Demo Stack..."

# Check if docker-compose or docker compose is available
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
else
    DOCKER_COMPOSE="docker compose"
fi

# Run the demo stack
$DOCKER_COMPOSE -f compose.demo.yaml up -d --remove-orphans

echo ""
echo "Platform is starting!"
echo "--------------------------------------------------"
echo "Gateway Service: http://api.iqkv.local/"
echo "Admin UI:        http://admin.iqkv.local/"
echo "App UI:          http://app.iqkv.local/"
echo "--------------------------------------------------"
echo "Services Monitoring (Subpaths):"
echo "RabbitMQ:        http://api.iqkv.local/services/rabbitmq/"
echo "Grafana:         http://api.iqkv.local/services/grafana/"
echo "Prometheus:      http://api.iqkv.local/services/prometheus/"
echo "MailHog:         http://api.iqkv.local/services/mailhog/"
echo "--------------------------------------------------"
echo "Note: Ensure you have added the domains to your /etc/hosts file."
