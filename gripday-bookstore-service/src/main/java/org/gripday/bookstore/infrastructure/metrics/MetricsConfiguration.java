package org.gripday.bookstore.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class MetricsConfiguration {
    
    private final BookstoreMetrics bookstoreMetrics;
    
    public MetricsConfiguration(BookstoreMetrics bookstoreMetrics) {
        this.bookstoreMetrics = bookstoreMetrics;
    }
    
    @Bean
    public MeterRegistry meterRegistryCustomizer(MeterRegistry registry) {
        registry.config().commonTags(
            "application", "bookstore-service",
            "service", "bookstore",
            "version", "1.0.0"
        );
        return registry;
    }
    
    // Refresh inventory metrics every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void refreshInventoryMetrics() {
        bookstoreMetrics.refreshInventoryMetrics();
    }
}