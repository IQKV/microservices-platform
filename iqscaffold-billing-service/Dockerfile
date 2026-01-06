# Multi-stage build for Billing Service with optimized caching
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven for build optimization
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy Maven configuration files first for better layer caching
COPY pom.xml .
COPY iqscaffold-billing-service/pom.xml iqscaffold-billing-service/

# Download dependencies in separate layer for better caching
RUN mvn dependency:go-offline -pl iqscaffold-billing-service -B

# Copy source code
COPY iqscaffold-billing-service/src iqscaffold-billing-service/src

# Build the application with optimizations
RUN mvn clean package -pl iqscaffold-billing-service -DskipTests -B && \
    # Extract JAR layers for better Docker layer caching
    mkdir -p target/dependency && \
    cd iqscaffold-billing-service/target && \
    java -Djarmode=layertools -jar iqscaffold-billing-service-*.jar extract --destination ../target/dependency

# Production runtime stage with security hardening
FROM eclipse-temurin:21-jre-alpine

# Security: Install only essential packages and remove package manager
RUN apk add --no-cache curl tzdata && \
    rm -rf /var/cache/apk/* && \
    # Create application user with restricted permissions
    addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser -s /bin/false -h /app

# Set working directory and create necessary directories
WORKDIR /app
RUN mkdir -p /app/logs /app/tmp && \
    chown -R appuser:appuser /app

# Copy application layers for optimal caching
COPY --from=builder --chown=appuser:appuser /app/target/dependency/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/dependency/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /app/target/dependency/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/dependency/application/ ./

# Switch to non-root user for security
USER appuser

# Expose application port
EXPOSE 8080

# Health check with improved configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/readiness || exit 1

# JVM optimization for billing service workloads
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true \
    -Dlogging.config=classpath:logback-spring.xml"

# Security: Set temporary directory
ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/app/tmp"

# Run application with Spring Boot's layered JAR approach
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_TOOL_OPTIONS org.springframework.boot.loader.launch.JarLauncher"]
