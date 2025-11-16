#!/bin/bash
set -e

HOST="${1:-localhost}"
PORT="${2:-8080}"
TIMEOUT="${3:-300}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

command -v curl > /dev/null || err "curl not found"

HEALTH_URL="http://$HOST:$PORT/actuator/health"
END=$(($(date +%s) + TIMEOUT))

log "Waiting for gateway at $HOST:$PORT (timeout: ${TIMEOUT}s)..."

while [ $(date +%s) -lt $END ]; do
    if curl -sf "$HEALTH_URL" | grep -q '"status":"UP"'; then
        log "Gateway is healthy"
        exit 0
    fi
    sleep 5
done

err "Timeout reached. Gateway not healthy."
