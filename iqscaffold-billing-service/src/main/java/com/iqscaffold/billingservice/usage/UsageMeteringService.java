package com.iqscaffold.billingservice.usage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for usage metering and quota enforcement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageMeteringService {

  private final UsageRecordRepository usageRecordRepository;

  // Service methods will be added in subsequent tasks
}
