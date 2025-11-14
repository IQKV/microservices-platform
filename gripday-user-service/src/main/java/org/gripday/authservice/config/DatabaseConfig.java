package org.gripday.authservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for User Service. Configures JPA repositories, entity scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = "org.gripday.authservice.infrastructure.repository")
@EntityScan(basePackages = "org.gripday.authservice.infrastructure.entity")
@EnableTransactionManagement
public class DatabaseConfig {
  // JPA configuration will be enhanced in subsequent tasks
}