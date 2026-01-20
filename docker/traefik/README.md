# Traefik Configuration for IQ Scaffold Backend

This directory contains the Traefik reverse proxy configuration for the IQ Scaffold backend services.

## Overview

Traefik acts as a reverse proxy and load balancer, providing a single entry point for all backend services. It automatically discovers services through Docker labels and routes traffic accordingly.

## Service Access

### Main Entry Points

- **Port 80 (HTTP)**: Main web traffic, routes to gateway service
- **Port 8080**: Traefik dashboard and direct service access
- **Port 443 (HTTPS)**: Secure traffic (with SSL certificates)

### Service URLs

When running locally, you can access services through:

#### Via Gateway Service (Recommended)

- **Gateway**: `http://localhost` or `http://gateway.localhost`
  - Routes to all downstream services through the gateway

#### Direct Service Access (Development)

- **User Service**: `http://user-service.localhost:8080` or `http://localhost:8080/api/users`
- **Billing Service**: `http://billing-service.localhost:8080` or `http://localhost:8080/api/billing`
- **Contact Service**: `http://contact-service.localhost:8080` or `http://localhost:8080/api/contacts`
- **Lead Service**: `http://lead-service.localhost:8080` or `http://localhost:8080/api/leads`
- **Pipeline Service**: `http://pipeline-service.localhost:8080` or `http://localhost:8080/api/pipeline`

#### Observability Services

- **Traefik Dashboard**: `http://traefik.localhost` or `http://localhost:8080`
- **Prometheus**: `http://prometheus.localhost` or `http://localhost:9090`
- **Grafana**: `http://grafana.localhost` or `http://localhost:3000`
- **Loki**: `http://loki.localhost` or `http://localhost:3100`

#### Infrastructure Services

- **RabbitMQ Management**: `http://localhost:15672`
- **MailHog**: `http://localhost:8025`

## Configuration Files

### traefik.yml

Static configuration file that defines:

- Entry points (ports 80, 443, 8080)
- Docker provider configuration
- API dashboard settings
- SSL certificate resolvers
- Logging and metrics

## Docker Labels

Services are configured using Docker labels in the compose file:

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.service-name.rule=Host(`service.localhost`)"
  - "traefik.http.routers.service-name.entrypoints=web"
  - "traefik.http.services.service-name.loadbalancer.server.port=8080"
```

## Host File Configuration (Optional)

For better local development experience, add these entries to your `/etc/hosts` file:

```
127.0.0.1 localhost
127.0.0.1 gateway.localhost
127.0.0.1 user-service.localhost
127.0.0.1 billing-service.localhost
127.0.0.1 contact-service.localhost
127.0.0.1 lead-service.localhost
127.0.0.1 pipeline-service.localhost
127.0.0.1 traefik.localhost
127.0.0.1 prometheus.localhost
127.0.0.1 grafana.localhost
127.0.0.1 loki.localhost
```

## SSL/TLS Configuration

The configuration includes Let's Encrypt integration for automatic SSL certificates in production. For local development, services run over HTTP.

## Monitoring

Traefik exposes Prometheus metrics at `/metrics` endpoint, which are automatically scraped by the Prometheus instance in the stack.

## Troubleshooting

1. **Service not accessible**: Check if the service has proper Traefik labels and is running
2. **Traefik dashboard not loading**: Ensure port 8080 is not blocked and Traefik container is running
3. **SSL issues**: Check certificate resolver configuration and domain settings

## Production Considerations

- Update the email in `certificatesResolvers.letsencrypt.acme.email`
- Configure proper domain names instead of `.localhost`
- Set up proper SSL certificates
- Configure rate limiting and security headers
- Set up monitoring and alerting
