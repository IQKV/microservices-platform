package org.gripday.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for Gripday Auth Service.
 * 
 * This service provides centralized authentication, authorization, and user management
 * for the Gripday microservices platform using JWT tokens and Spring Security.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@ConfigurationPropertiesScan(basePackages = "org.gripday.authservice.config")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}