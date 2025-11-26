package com.iqscaffold.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for IQ Scaffold User Service.
 * <p>
 * This service provides centralized authentication, authorization, and user management for the IQ Scaffold microservices platform using JWT tokens and Spring Security.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.iqscaffold.userservice.infrastructure.config")
public class UserServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
}