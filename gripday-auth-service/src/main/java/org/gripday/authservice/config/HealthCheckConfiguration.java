package org.gripday.authservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for custom health checks in the authentication service.
 * Provides health indicators for database, Redis, and service-specific components.
 */
@Configuration
@EnableConfigurationProperties(GripdayObservabilityProperties.class)
public class HealthCheckConfiguration {

    /**
     * Custom health indicator for database connectivity and performance.
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return new DatabaseHealthIndicator(dataSource);
    }

    /**
     * Custom health indicator for Redis connectivity and performance.
     */
    @Bean
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return new RedisHealthIndicator(redisConnectionFactory);
    }

    /**
     * Custom health indicator for JWT token service functionality.
     */
    @Bean
    public HealthIndicator jwtServiceHealthIndicator() {
        return new JwtServiceHealthIndicator();
    }

    /**
     * Database health indicator implementation.
     */
    public static class DatabaseHealthIndicator implements HealthIndicator {
        
        private final JdbcTemplate jdbcTemplate;
        
        public DatabaseHealthIndicator(DataSource dataSource) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }
        
        @Override
        public Health health() {
            try {
                var startTime = System.currentTimeMillis();
                
                // Test database connectivity with a simple query
                var result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                
                var responseTime = System.currentTimeMillis() - startTime;
                
                if (result != null && result == 1) {
                    return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("responseTimeMs", responseTime)
                        .withDetail("status", "Connected")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("responseTimeMs", responseTime)
                        .withDetail("status", "Query failed")
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }

    /**
     * Redis health indicator implementation.
     */
    public static class RedisHealthIndicator implements HealthIndicator {
        
        private final RedisConnectionFactory redisConnectionFactory;
        
        public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
            this.redisConnectionFactory = redisConnectionFactory;
        }
        
        @Override
        public Health health() {
            try {
                var startTime = System.currentTimeMillis();
                
                // Test Redis connectivity
                var connection = redisConnectionFactory.getConnection();
                var pong = connection.ping();
                connection.close();
                
                var responseTime = System.currentTimeMillis() - startTime;
                
                if ("PONG".equals(pong)) {
                    return Health.up()
                        .withDetail("redis", "Connected")
                        .withDetail("responseTimeMs", responseTime)
                        .withDetail("status", "Available")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("redis", "Ping failed")
                        .withDetail("responseTimeMs", responseTime)
                        .withDetail("status", "Unavailable")
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail("redis", "Connection failed")
                    .withDetail("status", "Unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }

    /**
     * JWT service health indicator implementation.
     */
    public static class JwtServiceHealthIndicator implements HealthIndicator {
        
        @Override
        public Health health() {
            try {
                // Test JWT service functionality by checking if we can access JWT configuration
                var jwtSecretPresent = System.getenv("GRIPDAY_AUTH_JWT_SECRET") != null || 
                                     System.getProperty("gripday.auth.jwt.secret-key") != null;
                
                if (jwtSecretPresent) {
                    return Health.up()
                        .withDetail("jwtService", "Available")
                        .withDetail("status", "JWT configuration present")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("jwtService", "Configuration missing")
                        .withDetail("status", "JWT secret not configured")
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail("jwtService", "Error")
                    .withDetail("status", "JWT service check failed")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }
}