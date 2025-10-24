package org.gripday.bookstore.infrastructure.metrics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricsScheduler {
    
    private final BookstoreMetrics bookstoreMetrics;
    
    public MetricsScheduler(BookstoreMetrics bookstoreMetrics) {
        this.bookstoreMetrics = bookstoreMetrics;
    }
    
    // Refresh inventory metrics every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void refreshInventoryMetrics() {
        bookstoreMetrics.refreshInventoryMetrics();
    }
}