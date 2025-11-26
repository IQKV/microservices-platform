package com.iqscaffold.userservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for User Service. Configures JPA repositories, entity scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.iqscaffold.userservice.usermanagement",
    "com.iqscaffold.userservice.tenancy",
    "com.iqscaffold.userservice.organization",
    "com.iqscaffold.userservice.emailverification",
    "com.iqscaffold.userservice.security",
    "com.iqscaffold.userservice.shared"
})
@EntityScan(basePackages = {
    "com.iqscaffold.userservice.usermanagement",
    "com.iqscaffold.userservice.tenancy",
    "com.iqscaffold.userservice.organization",
    "com.iqscaffold.userservice.emailverification",
    "com.iqscaffold.userservice.security",
    "com.iqscaffold.userservice.shared"
})
@EnableTransactionManagement
public class DatabaseConfig {
  // Entities and repositories are organized by domain modules
}