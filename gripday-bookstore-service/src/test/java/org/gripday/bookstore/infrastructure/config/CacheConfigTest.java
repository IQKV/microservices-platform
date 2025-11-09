package org.gripday.bookstore.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest {

  @Autowired
  private CacheManager cacheManager;

  @Test
  void shouldConfigureCacheManager() {
    assertThat(cacheManager).isNotNull();

    // Verify cache names are configured
    var cacheNames = cacheManager.getCacheNames();
    assertThat(cacheNames).contains(
        CacheConfig.BOOK_CACHE,
        CacheConfig.BOOK_SEARCH_CACHE,
        CacheConfig.CATEGORY_CACHE,
        CacheConfig.AUTHOR_CACHE,
        CacheConfig.POPULAR_BOOKS_CACHE
    );
  }

  @Test
  void shouldCreateCacheInstances() {
    var bookCache = cacheManager.getCache(CacheConfig.BOOK_CACHE);
    assertThat(bookCache).isNotNull();

    var searchCache = cacheManager.getCache(CacheConfig.BOOK_SEARCH_CACHE);
    assertThat(searchCache).isNotNull();

    var categoryCache = cacheManager.getCache(CacheConfig.CATEGORY_CACHE);
    assertThat(categoryCache).isNotNull();
  }
}