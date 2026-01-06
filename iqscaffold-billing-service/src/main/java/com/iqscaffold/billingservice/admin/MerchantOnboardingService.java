package com.iqscaffold.billingservice.admin;

import java.util.Optional;

/**
 * Interface for managing merchant Stripe Connect onboarding flow.
 * <p>
 * This interface defines the contract for merchant onboarding operations including:
 * <ul>
 *   <li>Stripe Connect account creation and management</li>
 *   <li>Onboarding link generation</li>
 *   <li>Merchant status tracking</li>
 *   <li>Multi-tenant merchant configuration</li>
 * </ul>
 * 
 * <h4>Key Features:</h4>
 * <ul>
 *   <li><strong>Multi-tenant Support</strong> - Tenant-isolated merchant configurations</li>
 *   <li><strong>Provider Integration</strong> - Abstracted payment provider interactions</li>
 *   <li><strong>Status Tracking</strong> - Comprehensive onboarding status management</li>
 *   <li><strong>Notification System</strong> - Email notifications for onboarding steps</li>
 * </ul>
 * 
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public interface MerchantOnboardingService {

    /**
     * Initiates the merchant onboarding process.
     * <p>
     * Process Flow:
     * <ol>
     *   <li>Checks if merchant is already fully onboarded</li>
     *   <li>Creates or retrieves existing Stripe Connect account</li>
     *   <li>Generates onboarding link for merchant setup</li>
     *   <li>Sends email notification with onboarding instructions</li>
     * </ol>
     *
     * @param refreshUrl URL to redirect if onboarding link expires
     * @param returnUrl URL to redirect after onboarding completion
     * @return The onboarding URL for the merchant to complete setup
     * @throws IllegalStateException If merchant is already fully onboarded
     */
    String initiateOnboarding(String refreshUrl, String returnUrl);

    /**
     * Retrieves the current merchant configuration and onboarding status.
     * 
     * @return Optional containing the merchant configuration if exists, empty otherwise
     */
    Optional<MerchantStripeConfig> getMerchantStatus();
}