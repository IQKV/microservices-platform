package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@DisplayName("CacheConfig Tests")
class CacheConfigTest {

  @Test
  @DisplayName("Should create Redis cache configuration with default TTL")
  void shouldCreateRedisCacheConfigurationWithDefaultTtl() {
    // Arrange
    var config = new CacheConfig();

    // Act
    var cacheConfig = config.redisCacheConfiguration();

    // Assert
    assertThat(cacheConfig).isNotNull();
    assertThat(cacheConfig.getTtl()).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  @DisplayName("Should create Redis template with connection factory")
  void shouldCreateRedisTemplateWithConnectionFactory() {
    // Arrange
    var config = new CacheConfig();
    var connectionFactory = mock(RedisConnectionFactory.class);

    // Act
    var redisTemplate = config.redisTemplate(connectionFactory);

    // Assert
    assertThat(redisTemplate).isNotNull();
    assertThat(redisTemplate.getConnectionFactory()).isEqualTo(connectionFactory);
  }

  @Test
  @DisplayName("Should verify cache name constants")
  void shouldVerifyCacheNameConstants() {
    // Assert
    assertThat(CacheConfig.BOOK_CACHE).isEqualTo("books");
    assertThat(CacheConfig.BOOK_SEARCH_CACHE).isEqualTo("book-search");
    assertThat(CacheConfig.CATEGORY_CACHE).isEqualTo("categories");
    assertThat(CacheConfig.AUTHOR_CACHE).isEqualTo("authors");
    assertThat(CacheConfig.POPULAR_BOOKS_CACHE).isEqualTo("popular-books");
  }
}
