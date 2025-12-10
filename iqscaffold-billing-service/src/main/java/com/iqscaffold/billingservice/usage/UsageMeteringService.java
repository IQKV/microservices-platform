package com.iqscaffold.billingservice.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service layer for usage metering and quota enforcement.
 */
@Service
public class UsageMeteringService {

  private static final Logger log = LoggerFactory.getLogger(UsageMeteringService.class);

  private final UsageRecordRepository usageRecordRepository;

  public UsageMeteringService(UsageRecordRepository usageRecordRepository) {
    this.usageRecordRepository = usageRecordRepository;
  }

  // Service methods will be added in subsequent tasks
}
