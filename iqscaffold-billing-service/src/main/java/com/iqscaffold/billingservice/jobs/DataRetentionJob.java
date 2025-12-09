package com.iqscaffold.billingservice.jobs;

import com.iqscaffold.billingservice.compliance.GdprComplianceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to apply data retention policies.
 * Runs daily to clean up old data according to GDPR and compliance requirements.
 */
@Component
public class DataRetentionJob {

  private static final Logger logger = LoggerFactory.getLogger(DataRetentionJob.class);

  private final GdprComplianceService gdprComplianceService;

  public DataRetentionJob(GdprComplianceService gdprComplianceService) {
    this.gdprComplianceService = gdprComplianceService;
  }

  /**
   * Apply data retention policies daily at 2 AM.
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void applyRetentionPolicies() {
    logger.info("Starting data retention job");

    try {
      gdprComplianceService.applyRetentionPolicies();
      logger.info("Data retention job completed successfully");
    } catch (final Exception e) {
      logger.error("Data retention job failed", e);
    }
  }
}
