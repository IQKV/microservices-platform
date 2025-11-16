#!/bin/bash
set -e

echo "🛑 Stopping platform..."
docker-compose down
echo "✅ Stopped (use -v flag to remove volumes)"