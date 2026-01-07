package com.iqscaffold.billingservice.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheManagementServiceTest {

  @Mock
  private EntityManagerFactory entityManagerFactory;

  @Mock
  private SessionFactory sessionFactory;

  @Mock
  private Cache cache;

  @Mock
  private Statistics statistics;

  private CacheManagementService cacheManagementService;

  @BeforeEach
  void setUp() {
    when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);

    cacheManagementService = new CacheManagementService(entityManagerFactory);
  }

  @Test
  void evictAllCaches_shouldEvictAllRegions() {
    // When
    when(sessionFactory.getCache()).thenReturn(cache);
    assertDoesNotThrow(() -> cacheManagementService.evictAllCaches());

    // Then
    verify(cache).evictAllRegions();
  }

  @Test
  void evictEntityCache_shouldEvictSpecificEntity() {
    // Given
    Class<?> entityClass = String.class;
    when(sessionFactory.getCache()).thenReturn(cache);

    // When
    assertDoesNotThrow(() -> cacheManagementService.evictEntityCache(entityClass));

    // Then
    verify(cache).evictEntityData(entityClass);
  }

  @Test
  void evictEntity_shouldEvictSpecificEntityInstance() {
    // Given
    Class<?> entityClass = String.class;
    Object id = 123L;
    when(sessionFactory.getCache()).thenReturn(cache);

    // When
    assertDoesNotThrow(() -> cacheManagementService.evictEntity(entityClass, id));

    // Then
    verify(cache).evictEntityData(entityClass, id);
  }

  @Test
  void evictQueryCaches_shouldEvictAllQueryRegions() {
    // When
    when(sessionFactory.getCache()).thenReturn(cache);
    assertDoesNotThrow(() -> cacheManagementService.evictQueryCaches());

    // Then
    verify(cache).evictQueryRegions();
  }

  @Test
  void getCacheStatistics_shouldReturnStatisticsMap() {
    // Given
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(true);
    when(statistics.getSecondLevelCacheHitCount()).thenReturn(100L);
    when(statistics.getSecondLevelCacheMissCount()).thenReturn(20L);
    when(statistics.getSecondLevelCachePutCount()).thenReturn(50L);
    when(statistics.getQueryCacheHitCount()).thenReturn(30L);
    when(statistics.getQueryCacheMissCount()).thenReturn(10L);
    when(statistics.getQueryCachePutCount()).thenReturn(15L);

    // When
    Map<String, Object> result = cacheManagementService.getCacheStatistics();

    // Then
    assertNotNull(result);
    assertEquals(true, result.get("statisticsEnabled"));
    assertEquals(100L, result.get("secondLevelCacheHitCount"));
    assertEquals(20L, result.get("secondLevelCacheMissCount"));
    assertEquals(50L, result.get("secondLevelCachePutCount"));
    assertEquals(30L, result.get("queryCacheHitCount"));
    assertEquals(10L, result.get("queryCacheMissCount"));
    assertEquals(15L, result.get("queryCachePutCount"));
  }

  @Test
  void getCacheStatistics_shouldHandleDisabledStatistics() {
    // Given
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.isStatisticsEnabled()).thenReturn(false);
    when(statistics.getSecondLevelCacheHitCount()).thenReturn(0L);
    when(statistics.getSecondLevelCacheMissCount()).thenReturn(0L);
    when(statistics.getSecondLevelCachePutCount()).thenReturn(0L);
    when(statistics.getQueryCacheHitCount()).thenReturn(0L);
    when(statistics.getQueryCacheMissCount()).thenReturn(0L);
    when(statistics.getQueryCachePutCount()).thenReturn(0L);

    // When
    Map<String, Object> result = cacheManagementService.getCacheStatistics();

    // Then
    assertNotNull(result);
    assertEquals(false, result.get("statisticsEnabled"));
  }

  @Test
  void getRegionStatistics_shouldReturnRegionStats() {
    // Given
    String regionName = "com.example.Entity";
    CacheRegionStatistics regionStats = mock(CacheRegionStatistics.class);
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    when(statistics.getCacheRegionStatistics(regionName)).thenReturn(regionStats);

    // When
    CacheRegionStatistics result = cacheManagementService.getRegionStatistics(regionName);

    // Then
    assertNotNull(result);
    assertEquals(regionStats, result);
    verify(statistics).getCacheRegionStatistics(regionName);
  }

  @Test
  void setStatisticsEnabled_shouldEnableStatistics() {
    // When
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    assertDoesNotThrow(() -> cacheManagementService.setStatisticsEnabled(true));

    // Then
    verify(statistics).setStatisticsEnabled(true);
  }

  @Test
  void setStatisticsEnabled_shouldDisableStatistics() {
    // When
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    assertDoesNotThrow(() -> cacheManagementService.setStatisticsEnabled(false));

    // Then
    verify(statistics).setStatisticsEnabled(false);
  }

  @Test
  void clearStatistics_shouldClearAllStats() {
    // When
    when(sessionFactory.getStatistics()).thenReturn(statistics);
    assertDoesNotThrow(() -> cacheManagementService.clearStatistics());

    // Then
    verify(statistics).clear();
  }
}
