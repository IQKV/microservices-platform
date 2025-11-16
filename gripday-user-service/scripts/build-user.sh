#!/bin/bash
set -e

PROFILE="${1:-local}"
SKIP_TESTS="${2:-false}"
CLEAN="${3:-true}"

log() { echo "$(tput setaf 4)[INFO]$(tput sgr0) $1"; }
err() { echo "$(tput setaf 1)[ERROR]$(tput sgr0) $1" >&2; exit 1; }

[[ "$PROFILE" =~ ^(local|staging|production)$ ]] || err "Invalid profile: $PROFILE"
[[ "$SKIP_TESTS" =~ ^(true|false)$ ]] || err "Invalid SKIP_TESTS: $SKIP_TESTS"
[[ "$CLEAN" =~ ^(true|false)$ ]] || err "Invalid CLEAN: $CLEAN"

command -v java > /dev/null || err "Java not found"
command -v mvn > /dev/null || err "Maven not found"

log "Building user service (profile: $PROFILE, tests: $SKIP_TESTS, clean: $CLEAN)"

cd "$(dirname "$0")/.."

[ "$CLEAN" = "true" ] && mvn clean -q

ARGS="-Dspring.profiles.active=$PROFILE"
[ "$SKIP_TESTS" = "true" ] && ARGS="$ARGS -DskipTests"

mvn package $ARGS

JAR=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)
[ -n "$JAR" ] && log "Built: $JAR ($(du -h "$JAR" | cut -f1))" || err "JAR not found"
