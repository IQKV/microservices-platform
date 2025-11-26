package com.iqscaffold.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryPerformanceService Tests")
class QueryPerformanceServiceTest {

  @Mock
  private EntityManagerFactory entityManagerFactory;

  @Mock
  private SessionFactory sessionFactory;

  @Mock
  private Statistics statistics;

  private QueryPerformanceService queryPerformanceService;

  @BeforeEach
  void setUp() {
    queryPerformanceService = new QueryPerformanceService(entityManagerFactory);
  }

  @Test
  @DisplayName("Should collect performance metrics successfully")
  void shouldCollectPerformanceMetricsSuccessfully() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getQueryExecutionCount()).thenReturn(100L);
    when(statistics.getQueryExecutionMaxTime()).thenReturn(500L);
    when(statistics.getQueryExecutionMaxTimeQueryString()).thenReturn("SELECT * FROM books");
    when(statistics.getQueryCacheHitCount()).thenReturn(80L);
    when(statistics.getQueryCacheMissCount()).thenReturn(20L);
    when(statistics.getSecondLevelCacheHitCount()).thenReturn(150L);
    when(statistics.getSecondLevelCacheMissCount()).thenReturn(50L);
    when(statistics.getConnectCount()).thenReturn(10L);
    when(statistics.getFlushCount()).thenReturn(5L);
    when(statistics.getTransactionCount()).thenReturn(50L);
    when(statistics.getSuccessfulTransactionCount()).thenReturn(48L);

    // Act
    queryPerformanceService.collectPerformanceMetrics();

    // Assert
    var metrics = queryPerformanceService.getPerformanceMetrics();
    assertThat(metrics).isNotEmpty();
    assertThat(metrics.get("queryExecutionCount")).isEqualTo(100L);
    assertThat(metrics.get("queryExecutionMaxTime")).isEqualTo(500L);
    assertThat(metrics.get("queryCacheHitRatio")).isEqualTo(0.8);
  }

  @Test
  @DisplayName("Should handle disabled statistics")
  void shouldHandleDisabledStatistics() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(false);

    // Act
    queryPerformanceService.collectPerformanceMetrics();

    // Assert
    var metrics = queryPerformanceService.getPerformanceMetrics();
    assertThat(metrics).isEmpty();
  }

  @Test
  @DisplayName("Should handle exception during metrics collection")
  void shouldHandleExceptionDuringMetricsCollection() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class))
        .thenThrow(new RuntimeException("Database error"));

    // Act - Should not throw exception
    queryPerformanceService.collectPerformanceMetrics();

    // Assert - Should have empty metrics
    var metrics = queryPerformanceService.getPerformanceMetrics();
    assertThat(metrics).isEmpty();
  }

  @Test
  @DisplayName("Should calculate hit ratio correctly")
  void shouldCalculateHitRatioCorrectly() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getQueryCacheHitCount()).thenReturn(75L);
    when(statistics.getQueryCacheMissCount()).thenReturn(25L);
    when(statistics.getSecondLevelCacheHitCount()).thenReturn(0L);
    when(statistics.getSecondLevelCacheMissCount()).thenReturn(0L);

    // Act
    queryPerformanceService.collectPerformanceMetrics();

    // Assert
    var metrics = queryPerformanceService.getPerformanceMetrics();
    assertThat(metrics.get("queryCacheHitRatio")).isEqualTo(0.75);
    assertThat(metrics.get("secondLevelCacheHitRatio")).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Should reset statistics successfully")
  void shouldResetStatisticsSuccessfully() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);

    // Act
    queryPerformanceService.resetStatistics();

    // Assert
    verify(statistics).clear();
  }

  @Test
  @DisplayName("Should handle exception during statistics reset")
  void shouldHandleExceptionDuringStatisticsReset() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class))
        .thenThrow(new RuntimeException("Cannot reset"));

    // Act - Should not throw exception
    queryPerformanceService.resetStatistics();

    // Assert - No exception thrown
  }

  @Test
  @DisplayName("Should return healthy status with good metrics")
  void shouldReturnHealthyStatusWithGoodMetrics() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getQueryCacheHitCount()).thenReturn(90L);
    when(statistics.getQueryCacheMissCount()).thenReturn(10L);
    when(statistics.getQueryExecutionMaxTime()).thenReturn(200L);

    queryPerformanceService.collectPerformanceMetrics();

    // Act
    var healthStatus = queryPerformanceService.getHealthStatus();

    // Assert
    assertThat(healthStatus.get("status")).isEqualTo("HEALTHY");
    assertThat(healthStatus.get("queryCacheHitRatio")).isEqualTo("90.00%");
    assertThat(healthStatus.get("maxQueryTime")).isEqualTo("200ms");
  }

  @Test
  @DisplayName("Should return degraded status with poor cache hit ratio")
  void shouldReturnDegradedStatusWithPoorCacheHitRatio() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getQueryCacheHitCount()).thenReturn(40L);
    when(statistics.getQueryCacheMissCount()).thenReturn(60L);
    when(statistics.getQueryExecutionMaxTime()).thenReturn(300L);

    queryPerformanceService.collectPerformanceMetrics();

    // Act
    var healthStatus = queryPerformanceService.getHealthStatus();

    // Assert
    assertThat(healthStatus.get("status")).isEqualTo("DEGRADED");
    assertThat(healthStatus.get("reason")).isEqualTo("Performance degradation detected");
  }

  @Test
  @DisplayName("Should return degraded status with slow queries")
  void shouldReturnDegradedStatusWithSlowQueries() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getQueryCacheHitCount()).thenReturn(80L);
    when(statistics.getQueryCacheMissCount()).thenReturn(20L);
    when(statistics.getQueryExecutionMaxTime()).thenReturn(6000L);

    queryPerformanceService.collectPerformanceMetrics();

    // Act
    var healthStatus = queryPerformanceService.getHealthStatus();

    // Assert
    assertThat(healthStatus.get("status")).isEqualTo("DEGRADED");
  }

  @Test
  @DisplayName("Should return unknown status when no metrics available")
  void shouldReturnUnknownStatusWhenNoMetricsAvailable() {
    // Act
    var healthStatus = queryPerformanceService.getHealthStatus();

    // Assert
    assertThat(healthStatus.get("status")).isEqualTo("UNKNOWN");
    assertThat(healthStatus.get("reason")).isEqualTo("No metrics available");
  }

  @Test
  @DisplayName("Should return error status on exception")
  void shouldReturnErrorStatusOnException() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class))
        .thenThrow(new RuntimeException("Database connection failed"));

    queryPerformanceService.collectPerformanceMetrics();

    // Act
    var healthStatus = queryPerformanceService.getHealthStatus();

    // Assert
    assertThat(healthStatus.get("status")).isIn("UNKNOWN", "ERROR");
  }

  @Test
  @DisplayName("Should get performance metrics as copy")
  void shouldGetPerformanceMetricsAsCopy() {
    // Arrange
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getQueryExecutionCount()).thenReturn(50L);

    queryPerformanceService.collectPerformanceMetrics();

    // Act
    var metrics1 = queryPerformanceService.getPerformanceMetrics();
    var metrics2 = queryPerformanceService.getPerformanceMetrics();

    // Assert
    assertThat(metrics1).isNotSameAs(metrics2);
    assertThat(metrics1).isEqualTo(metrics2);
  }
}
