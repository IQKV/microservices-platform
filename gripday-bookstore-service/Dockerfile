# Multi-stage build for Spring Boot application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install required packages for build
RUN apk add --no-cache curl

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application with optimizations
RUN ./mvnw clean package -DskipTests -Dspring-boot.repackage.skip=false

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install required runtime packages
RUN apk add --no-cache \
    curl \
    wget \
    tzdata \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Create non-root user with specific UID/GID for consistency
RUN addgroup -g 1001 -S bookstore && \
    adduser -S bookstore -u 1001 -G bookstore

# Copy built JAR with specific name for consistency
COPY --from=builder /app/target/gripday-bookstore-service-*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R bookstore:bookstore /app

# Switch to non-root user
USER bookstore

# Health check with improved configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Expose port
EXPOSE 8082

# JVM optimization for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Application entrypoint with JVM optimizations
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]