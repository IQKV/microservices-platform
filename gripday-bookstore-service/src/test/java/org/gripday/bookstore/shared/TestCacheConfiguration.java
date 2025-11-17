package org.gripday.bookstore.shared;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestCacheConfiguration {

  @Bean
  @Primary
  @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
  public CacheManager testCacheManager() {
    var cacheManager = new ConcurrentMapCacheManager();
    // Pre-configure cache names for tests
    cacheManager.setCacheNames(Arrays.asList(
        CacheConfig.BOOK_CACHE,
        CacheConfig.BOOK_SEARCH_CACHE,
        CacheConfig.CATEGORY_CACHE,
        CacheConfig.AUTHOR_CACHE,
        CacheConfig.POPULAR_BOOKS_CACHE
    ));
    return cacheManager;
  }
}
