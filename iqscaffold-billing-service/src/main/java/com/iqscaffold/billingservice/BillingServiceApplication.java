package com.iqscaffold.billingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Billing & Subscription Management Service.
 *
 * <p>This service provides comprehensive subscription lifecycle management,
 * usage-based billing, payment processing, and invoice management capabilities.
 *
 * <p>Key features:
 * <ul>
 *   <li>Subscription lifecycle management (create, upgrade, downgrade, cancel)</li>
 *   <li>Multi-provider payment integration (Stripe, PayPal, manual)</li>
 *   <li>Automated invoice generation and PDF delivery</li>
 *   <li>Usage-based billing and quota enforcement</li>
 *   <li>Multi-tenancy with schema-per-tenant isolation</li>
 *   <li>Scheduled jobs for trial expiration, payment retry, invoice generation, usage reset, and trial reminders</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
