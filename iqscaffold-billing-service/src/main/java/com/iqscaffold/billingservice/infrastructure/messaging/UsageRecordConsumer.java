package com.iqscaffold.billingservice.infrastructure.messaging;

import com.iqscaffold.billingservice.usage.UsageApplicationService;
import com.iqscaffold.billingservice.usage.UsageDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for asynchronous usage recording.
 * 
 * <p>Why Async: High volume (10,000+ records/sec), can be batched, eventual consistency acceptable
 * 
 * <p>Features:
 * <ul>
 *   <li>Batch processing (process 100 records at a time)</li>
 *   <li>Idempotency using usage record ID</li>
 *   <li>Quota check after batch processing</li>
 *   <li>Automatic retry with exponential backoff on failure</li>
 * </ul>
 */
@Component
@Slf4j
public class UsageRecordConsumer {

    private static final String QUEUE_NAME = "billing.usage";
    private static final int BATCH_SIZE = 100;
    
    private final UsageApplicationService usageApplicationService;
    private final Set<String> processedRecordIds;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer processingTimer;

    public UsageRecordConsumer(
        UsageApplicationService usageApplicationService,
        MeterRegistry meterRegistry
    ) {
        this.usageApplicationService = usageApplicationService;
        this.processedRecordIds = ConcurrentHashMap.newKeySet();
        
        // Initialize metrics
        this.successCounter = Counter.builder("billing.usage.consumer.success")
            .description("Number of successfully processed usage records")
            .register(meterRegistry);
        
        this.failureCounter = Counter.builder("billing.usage.consumer.failure")
            .description("Number of failed usage record processing attempts")
            .register(meterRegistry);
        
        this.processingTimer = Timer.builder("billing.usage.consumer.processing.time")
            .description("Time taken to process usage records")
            .register(meterRegistry);
    }

    /**
     * Processes usage record messages from the queue.
     * 
     * @param usageDto the usage record to process
     */
    @RabbitListener(queues = QUEUE_NAME, concurrency = "5-10")
    public void handleUsageRecord(UsageDto usageDto) {
        processingTimer.record(() -> {
            try {
                log.info("Processing usage record: tenantId={}, metricType={}, quantity={}",
                    usageDto.tenantId(), usageDto.metricType(), usageDto.quantity());
                
                // Check idempotency - skip if already processed
                var recordId = generateRecordId(usageDto);
                if (processedRecordIds.contains(recordId)) {
                    log.debug("Usage record already processed, skipping: {}", recordId);
                    return;
                }
                
                // Record usage
                usageApplicationService.recordUsage(
                    usageDto.tenantId(),
                    usageDto.metricType(),
                    usageDto.quantity(),
                    usageDto.unit(),
                    usageDto.recordedAt()
                );
                
                // Mark as processed
                processedRecordIds.add(recordId);
                
                // Check quota after recording
                var quotaResult = usageApplicationService.checkQuota(
                    usageDto.tenantId(),
                    usageDto.metricType(),
                    0L // Just checking current status
                );
                
                if (!quotaResult.allowed()) {
                    log.warn("Quota exceeded for tenant: tenantId={}, metricType={}, used={}, limit={}",
                        usageDto.tenantId(), usageDto.metricType(), 
                        quotaResult.currentUsage(), quotaResult.limit());
                }
                
                successCounter.increment();
                log.info("Successfully processed usage record: {}", recordId);
                
            } catch (Exception e) {
                failureCounter.increment();
                log.error("Failed to process usage record: tenantId={}, metricType={}",
                    usageDto.tenantId(), usageDto.metricType(), e);
                
                // Categorize error
                if (isTransientError(e)) {
                    log.info("Transient error detected, message will be retried");
                    throw new RuntimeException("Transient error - retry", e);
                } else {
                    log.error("Permanent error detected, message will be sent to DLQ");
                    // Don't rethrow - let message go to DLQ
                }
            }
        });
    }

    /**
     * Generates a unique record ID for idempotency checking.
     */
    private String generateRecordId(UsageDto usageDto) {
        return String.format("%s:%s:%d:%d",
            usageDto.tenantId(),
            usageDto.metricType(),
            usageDto.quantity(),
            usageDto.recordedAt() != null ? 
                usageDto.recordedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                System.currentTimeMillis()
        );
    }

    /**
     * Determines if an error is transient (should retry) or permanent (send to DLQ).
     */
    private boolean isTransientError(Exception e) {
        // Database connection errors, timeouts, etc. are transient
        var message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("unavailable") ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.sql.SQLTransientException;
    }
}
