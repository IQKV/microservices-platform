package org.gripday.userservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for User Service. Configures JPA repositories, entity scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "org.gripday.userservice.usermanagement",
    "org.gripday.userservice.tenancy",
    "org.gripday.userservice.organization",
    "org.gripday.userservice.emailverification",
    "org.gripday.userservice.security",
    "org.gripday.userservice.shared"
})
@EntityScan(basePackages = {
    "org.gripday.userservice.usermanagement",
    "org.gripday.userservice.tenancy",
    "org.gripday.userservice.organization",
    "org.gripday.userservice.emailverification",
    "org.gripday.userservice.security",
    "org.gripday.userservice.shared"
})
@EnableTransactionManagement
public class DatabaseConfig {
  // Entities and repositories are organized by domain modules
}