package org.gripday.bookstore.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CacheConfigurationTest {

  @Autowired
  private CacheManager cacheManager;

  @Test
  void shouldConfigureCacheManager() {
    assertThat(cacheManager).isNotNull();

    // Verify cache names are configured
    var cacheNames = cacheManager.getCacheNames();
    assertThat(cacheNames).contains(
        CacheConfiguration.BOOK_CACHE,
        CacheConfiguration.BOOK_SEARCH_CACHE,
        CacheConfiguration.CATEGORY_CACHE,
        CacheConfiguration.AUTHOR_CACHE,
        CacheConfiguration.POPULAR_BOOKS_CACHE
    );
  }

  @Test
  void shouldCreateCacheInstances() {
    var bookCache = cacheManager.getCache(CacheConfiguration.BOOK_CACHE);
    assertThat(bookCache).isNotNull();

    var searchCache = cacheManager.getCache(CacheConfiguration.BOOK_SEARCH_CACHE);
    assertThat(searchCache).isNotNull();

    var categoryCache = cacheManager.getCache(CacheConfiguration.CATEGORY_CACHE);
    assertThat(categoryCache).isNotNull();
  }
}