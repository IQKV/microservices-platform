#!/bin/bash

# Stop Gripday Platform
# This script stops all platform services

set -e

echo "🛑 Stopping Gripday Microservices Platform..."

# Stop all services
docker-compose down

echo "✅ Platform stopped successfully!"
echo ""
echo "💡 To remove volumes as well: docker-compose down -v"
echo "🧹 To clean up everything: docker-compose down -v --remove-orphans"