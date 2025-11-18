---
name: optimize-performance
description: Comprehensive performance optimization for Spring Boot applications including JVM tuning, caching, and database optimization
---

# Performance Optimization Command

Perform comprehensive performance analysis and optimization for Spring Boot applications using modern Java features and best practices.

## Optimization Areas

### 1. JVM Performance Tuning

- **Garbage Collection**: Analyze and optimize GC settings (G1GC, ZGC, Parallel GC)
- **Memory Management**: Heap sizing, memory leak detection, off-heap storage
- **Virtual Threads**: Implement Project Loom for improved concurrency
- **Native Compilation**: GraalVM Native Image for faster startup and lower memory usage

### 2. Application Layer Optimization

- **Caching Strategy**: Implement multi-level caching with Spring Cache and Redis
- **Database Performance**: Query optimization, connection pooling, indexing
- **Async Processing**: Non-blocking operations with virtual threads and reactive programming
- **Resource Management**: Optimize connection pools, thread pools, and resource lifecycle

### 3. Monitoring and Profiling

- **Application Metrics**: Micrometer integration with Prometheus and Grafana
- **Performance Profiling**: JProfiler, VisualVM, async-profiler analysis
- **Distributed Tracing**: OpenTelemetry and Zipkin integration
- **Health Checks**: Custom health indicators and readiness probes

## Performance Analysis Checklist

### JVM Analysis

```bash
# Memory analysis
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCApplicationStoppedTime
-Xloggc:gc.log

# Virtual thread monitoring
-Djdk.tracePinnedThreads=full
-Djdk.virtualThreadScheduler.parallelism=8
```

### Database Performance

- **Query Analysis**: Identify slow queries and N+1 problems
- **Index Optimization**: Analyze query execution plans and add appropriate indexes
- **Connection Pooling**: Optimize HikariCP configuration
- **Batch Operations**: Implement batch processing for bulk operations

### Caching Strategy

- **Cache Levels**: L1 (application), L2 (distributed), L3 (database)
- **Cache Patterns**: Cache-aside, write-through, write-behind
- **Eviction Policies**: LRU, LFU, TTL-based eviction
- **Cache Warming**: Preload frequently accessed data

## Optimization Implementation

### Virtual Thread Configuration

```java
@Configuration
@EnableAsync
public class PerformanceConfig implements AsyncConfigurer {

  @Override
  public Executor getAsyncExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean
  public TaskExecutor virtualThreadTaskExecutor() {
    return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("vt-pool-", 0).factory()));
  }
}
```

### Caching Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    RedisCacheManager.Builder builder = RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory()).cacheDefaults(cacheConfiguration());

    return builder.build();
  }

  private RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(30))
      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
  }
}
```

### Database Optimization

```java
@Configuration
public class DatabaseConfig {

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  public HikariConfig hikariConfig() {
    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(20);
    config.setMinimumIdle(5);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);
    config.setLeakDetectionThreshold(60000);
    return config;
  }

  @Bean
  public DataSource dataSource() {
    return new HikariDataSource(hikariConfig());
  }
}
```

### Performance Monitoring

```java
@Component
public class PerformanceMonitor {

  private final MeterRegistry meterRegistry;

  public PerformanceMonitor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @EventListener
  public void handleRequest(RequestEvent event) {
    Timer.Sample sample = Timer.start(meterRegistry);
    sample.stop(Timer.builder("http.request.duration").tag("method", event.getMethod()).tag("uri", event.getUri()).register(meterRegistry));
  }

  @Scheduled(fixedRate = 60000)
  public void recordSystemMetrics() {
    Runtime runtime = Runtime.getRuntime();
    Gauge.builder("jvm.memory.used").register(meterRegistry, runtime, (r) -> r.totalMemory() - r.freeMemory());

    Gauge.builder("jvm.memory.free").register(meterRegistry, runtime, Runtime::freeMemory);
  }
}
```

## Performance Testing

### Load Testing Configuration

```yaml
# application-performance.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10

  cache:
    redis:
      time-to-live: 600000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### JMH Benchmarking

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class ServiceBenchmark {

  @Benchmark
  public void benchmarkServiceMethod() {
    // Performance test implementation
  }
}
```

## Optimization Execution Plan

### Phase 1: Analysis

1. **Profile Application**: Use profiling tools to identify bottlenecks
2. **Analyze Metrics**: Review application metrics and logs
3. **Database Analysis**: Examine query performance and execution plans
4. **Memory Analysis**: Check for memory leaks and GC pressure

### Phase 2: Implementation

1. **JVM Tuning**: Apply optimal JVM parameters
2. **Virtual Threads**: Migrate to virtual threads for I/O operations
3. **Caching**: Implement multi-level caching strategy
4. **Database Optimization**: Add indexes and optimize queries

### Phase 3: Validation

1. **Performance Testing**: Run load tests to validate improvements
2. **Monitoring**: Set up comprehensive monitoring and alerting
3. **Benchmarking**: Compare before and after performance metrics
4. **Documentation**: Document optimization techniques and results

## Expected Outcomes

### Performance Improvements

- **Startup Time**: 50-80% reduction with GraalVM Native Image
- **Memory Usage**: 30-50% reduction with optimized JVM settings
- **Throughput**: 2-5x improvement with virtual threads
- **Response Time**: 40-70% reduction with proper caching

### Monitoring Capabilities

- Real-time performance metrics
- Automated alerting for performance degradation
- Distributed tracing for request flow analysis
- Comprehensive health checks and readiness probes

Execute this command to perform a comprehensive performance optimization that leverages modern Java features and Spring Boot best practices.
