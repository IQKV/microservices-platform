package org.gripday.bookstore.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);
    
    @Bean(name = "cacheWarmupExecutor")
    public Executor cacheWarmupExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-warmup-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            logger.warn("Cache warmup task rejected, queue is full");
        });
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "performanceMonitoringExecutor")
    public Executor performanceMonitoringExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("perf-monitor-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            logger.warn("Performance monitoring task rejected, queue is full");
        });
        executor.initialize();
        return executor;
    }
}