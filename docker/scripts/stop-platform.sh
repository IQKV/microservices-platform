#!/bin/bash
set -e

echo "🛑 Stopping platform..."
docker compose down
echo "✅ Stopped"
echo "   To also remove volumes: docker compose down -v"
