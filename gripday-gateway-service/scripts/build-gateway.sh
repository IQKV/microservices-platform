#!/bin/bash

# Maven build script for Gripday Gateway Service
# Provides comprehensive build, test, and package functionality with environment-specific configurations

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default build configuration
DEFAULT_PROFILE="local"
DEFAULT_SKIP_TESTS="false"
DEFAULT_CLEAN="true"

# Build parameters
PROFILE="${1:-$DEFAULT_PROFILE}"
SKIP_TESTS="${2:-$DEFAULT_SKIP_TESTS}"
CLEAN="${3:-$DEFAULT_CLEAN}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Display usage information
usage() {
    echo "Usage: $0 [PROFILE] [SKIP_TESTS] [CLEAN]"
    echo
    echo "Parameters:"
    echo "  PROFILE    - Spring profile (local|staging|production, default: $DEFAULT_PROFILE)"
    echo "  SKIP_TESTS - Skip tests (true|false, default: $DEFAULT_SKIP_TESTS)"
    echo "  CLEAN      - Clean before build (true|false, default: $DEFAULT_CLEAN)"
    echo
    echo "Examples:"
    echo "  $0                          # Build with defaults"
    echo "  $0 staging                  # Build for staging"
    echo "  $0 production false true    # Production build with tests and clean"
    echo "  $0 local true false         # Local build, skip tests, no clean"
    echo
    echo "Environment Variables:"
    echo "  MAVEN_OPTS - Additional Maven options"
    echo "  JAVA_HOME  - Java installation directory"
}

# Check if help is requested
if [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

# Validate parameters
if [[ ! "$PROFILE" =~ ^(local|staging|production)$ ]]; then
    log_error "Invalid profile: $PROFILE"
    log_error "Valid profiles: local, staging, production"
    exit 1
fi

if [[ ! "$SKIP_TESTS" =~ ^(true|false)$ ]]; then
    log_error "Invalid SKIP_TESTS value: $SKIP_TESTS"
    log_error "Valid values: true, false"
    exit 1
fi

if [[ ! "$CLEAN" =~ ^(true|false)$ ]]; then
    log_error "Invalid CLEAN value: $CLEAN"
    log_error "Valid values: true, false"
    exit 1
fi

# Check prerequisites
check_prerequisites() {
    log_info "Checking build prerequisites..."
    
    # Check Java
    if [ -z "${JAVA_HOME:-}" ]; then
        log_warning "JAVA_HOME not set, using system Java"
    fi
    
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    local java_version
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 21 ]; then
        log_error "Java 21 or higher is required (found: $java_version)"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Display build configuration
display_build_config() {
    log_info "Build Configuration:"
    echo "  - Profile: $PROFILE"
    echo "  - Skip Tests: $SKIP_TESTS"
    echo "  - Clean Build: $CLEAN"
    echo "  - Project Directory: $PROJECT_DIR"
    echo "  - Java Version: $(java -version 2>&1 | head -n 1 | cut -d'"' -f2)"
    echo "  - Maven Version: $(mvn -version | head -n 1 | cut -d' ' -f3)"
    
    if [ -n "${MAVEN_OPTS:-}" ]; then
        echo "  - Maven Options: $MAVEN_OPTS"
    fi
}

# Clean build artifacts
clean_build() {
    if [ "$CLEAN" = "true" ]; then
        log_info "Cleaning build artifacts..."
        cd "$PROJECT_DIR"
        mvn clean -q
        log_success "Build artifacts cleaned"
    else
        log_info "Skipping clean (CLEAN=false)"
    fi
}

# Compile source code
compile_sources() {
    log_info "Compiling source code..."
    cd "$PROJECT_DIR"
    
    local maven_args="-Dspring.profiles.active=$PROFILE"
    
    if mvn compile $maven_args; then
        log_success "Source compilation completed"
    else
        log_error "Source compilation failed"
        exit 1
    fi
}

# Run tests
run_tests() {
    if [ "$SKIP_TESTS" = "false" ]; then
        log_info "Running tests..."
        cd "$PROJECT_DIR"
        
        local maven_args="-Dspring.profiles.active=$PROFILE"
        
        # Run unit tests
        if mvn test $maven_args; then
            log_success "Unit tests passed"
        else
            log_error "Unit tests failed"
            exit 1
        fi
        
        # Run integration tests
        if mvn verify $maven_args; then
            log_success "Integration tests passed"
        else
            log_error "Integration tests failed"
            exit 1
        fi
    else
        log_info "Skipping tests (SKIP_TESTS=true)"
    fi
}

# Package application
package_application() {
    log_info "Packaging application..."
    cd "$PROJECT_DIR"
    
    local maven_args="-Dspring.profiles.active=$PROFILE"
    
    if [ "$SKIP_TESTS" = "true" ]; then
        maven_args="$maven_args -DskipTests"
    fi
    
    if mvn package $maven_args; then
        log_success "Application packaging completed"
    else
        log_error "Application packaging failed"
        exit 1
    fi
}

# Generate build report
generate_build_report() {
    log_info "Generating build report..."
    cd "$PROJECT_DIR"
    
    local jar_file
    jar_file=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)
    
    if [ -n "$jar_file" ]; then
        local jar_size
        jar_size=$(du -h "$jar_file" | cut -f1)
        
        log_info "Build Report:"
        echo "  - JAR File: $jar_file"
        echo "  - JAR Size: $jar_size"
        echo "  - Build Profile: $PROFILE"
        echo "  - Tests Executed: $([ "$SKIP_TESTS" = "false" ] && echo "Yes" || echo "No")"
        echo "  - Build Time: $(date)"
    else
        log_warning "JAR file not found in target directory"
    fi
}

# Verify build artifacts
verify_build() {
    log_info "Verifying build artifacts..."
    cd "$PROJECT_DIR"
    
    local jar_file
    jar_file=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)
    
    if [ -z "$jar_file" ]; then
        log_error "JAR file not found in target directory"
        exit 1
    fi
    
    # Verify JAR file is executable
    if java -jar "$jar_file" --help &> /dev/null; then
        log_success "JAR file is executable"
    else
        log_warning "JAR file may not be executable (this is normal for some Spring Boot configurations)"
    fi
    
    # Check for required configuration files
    if jar tf "$jar_file" | grep -q "application.yml"; then
        log_success "Configuration files found in JAR"
    else
        log_warning "Configuration files not found in JAR"
    fi
    
    log_success "Build verification completed"
}

# Display next steps
display_next_steps() {
    local jar_file
    jar_file=$(find "$PROJECT_DIR/target" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)
    
    log_success "Build completed successfully!"
    echo
    log_info "Next Steps:"
    
    if [ -n "$jar_file" ]; then
        echo "  1. Run the application:"
        echo "     java -jar $jar_file"
        echo "     OR"
        echo "     mvn spring-boot:run -Dspring.profiles.active=$PROFILE"
        echo
        echo "  2. Build Docker image:"
        echo "     docker build -t gripday/gateway-service:$PROFILE ."
        echo
        echo "  3. Run with Docker Compose:"
        echo "     docker-compose -f docker-compose.$PROFILE.yml up"
    fi
    
    echo
    log_info "Useful Commands:"
    echo "  - View build logs: mvn help:system"
    echo "  - Run specific tests: mvn test -Dtest=TestClassName"
    echo "  - Generate test report: mvn surefire-report:report"
}

# Main execution
main() {
    log_info "Building Gripday Gateway Service"
    echo
    
    check_prerequisites
    display_build_config
    echo
    
    clean_build
    compile_sources
    run_tests
    package_application
    verify_build
    generate_build_report
    
    echo
    display_next_steps
}

# Handle script interruption
trap 'log_warning "Build interrupted"; exit 130' INT TERM

# Execute main function
main "$@"