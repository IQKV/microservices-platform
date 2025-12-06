package com.iqscaffold.billingservice.usage;

/**
 * Enumeration of metric types for usage tracking.
 * 
 * <p>Defines the different types of resources that can be metered for billing
 * and quota enforcement purposes. Each metric type represents a specific
 * resource or capability that can be consumed by tenants.
 * 
 * <p>Metric types are used in:
 * <ul>
 *   <li>Usage recording - tracking consumption</li>
 *   <li>Quota enforcement - limiting resource usage</li>
 *   <li>Billing calculations - metered billing</li>
 *   <li>Plan definitions - defining limits per plan tier</li>
 * </ul>
 */
public enum MetricType {
  
  /**
   * API calls across all platform services.
   * Unit: number of requests
   */
  API_CALLS,
  
  /**
   * Storage capacity in gigabytes.
   * Unit: GB
   */
  STORAGE_GB,
  
  /**
   * Number of emails sent through the email service.
   * Unit: number of emails
   */
  EMAIL_SENDS,
  
  /**
   * Number of campaign executions in the campaign service.
   * Unit: number of campaigns
   */
  CAMPAIGN_EXECUTIONS,
  
  /**
   * Number of scoring requests to the scoring service.
   * Unit: number of requests
   */
  SCORING_REQUESTS,
  
  /**
   * Number of active user seats.
   * Unit: number of users
   */
  ACTIVE_USERS,
  
  /**
   * Number of custom domains configured.
   * Unit: number of domains
   */
  CUSTOM_DOMAINS,
  
  /**
   * Number of data exports performed.
   * Unit: number of exports
   */
  DATA_EXPORTS,
  
  /**
   * Custom metric type for extensibility.
   * Allows tracking of additional metrics not predefined.
   * Unit: varies based on implementation
   */
  CUSTOM
}
