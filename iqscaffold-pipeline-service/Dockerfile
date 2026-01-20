# Multi-stage Dockerfile for IQ Scaffold Pipeline Service

# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy parent pom and service pom
COPY pom.xml ./
COPY iqscaffold-pipeline-service/pom.xml ./iqscaffold-pipeline-service/

# Download dependencies
RUN mvn dependency:go-offline -pl iqscaffold-pipeline-service

# Copy source code
COPY iqscaffold-pipeline-service/src ./iqscaffold-pipeline-service/src

# Build the application
RUN mvn clean package -pl iqscaffold-pipeline-service -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create app directory
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/iqscaffold-pipeline-service/target/iqscaffold-pipeline-service-*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/readiness || exit 1

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8085/actuator/health/readiness || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
