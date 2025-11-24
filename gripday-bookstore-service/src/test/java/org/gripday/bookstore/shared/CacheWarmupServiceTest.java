package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.gripday.bookstore.catalog.CatalogApplicationService;
import org.gripday.bookstore.catalog.SearchApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheWarmupService Tests")
class CacheWarmupServiceTest {

  @Mock
  private SearchApplicationService searchApplicationService;

  @Mock
  private CatalogApplicationService catalogApplicationService;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache bookCache;

  @Mock
  private Cache searchCache;

  @Mock
  private Cache popularCache;

  private CacheWarmupService cacheWarmupService;

  @BeforeEach
  void setUp() {
    cacheWarmupService = new CacheWarmupService(
        searchApplicationService,
        catalogApplicationService,
        cacheManager
    );
  }

  @Test
  @DisplayName("Should warmup cache on startup successfully")
  void shouldWarmupCacheOnStartupSuccessfully() {
    // Act
    cacheWarmupService.warmupCacheOnStartup();

    // Assert
    verify(catalogApplicationService).findAvailableBooks(any(Pageable.class));
    verify(catalogApplicationService).findBooksInStock(any(Pageable.class));
    verify(searchApplicationService).findRecentBooks(any(Pageable.class));
    verify(searchApplicationService).getDistinctAuthors();
    verify(searchApplicationService).getDistinctCategories();
  }

  @Test
  @DisplayName("Should handle exception during startup warmup")
  void shouldHandleExceptionDuringStartupWarmup() {
    // Arrange
    doThrow(new RuntimeException("Database error"))
        .when(catalogApplicationService).findAvailableBooks(any(Pageable.class));

    // Act - Should not throw exception
    cacheWarmupService.warmupCacheOnStartup();

    // Assert - Should have attempted the call
    verify(catalogApplicationService).findAvailableBooks(any(Pageable.class));
  }

  @Test
  @DisplayName("Should run scheduled cache warmup")
  void shouldRunScheduledCacheWarmup() {
    // Act
    cacheWarmupService.scheduledCacheWarmup();

    // Assert
    verify(catalogApplicationService).findAvailableBooks(any(Pageable.class));
    verify(catalogApplicationService).findBooksInStock(any(Pageable.class));
    verify(searchApplicationService).findRecentBooks(any(Pageable.class));
    verify(searchApplicationService).getDistinctAuthors();
    verify(searchApplicationService).getDistinctCategories();
  }

  @Test
  @DisplayName("Should handle exception during scheduled warmup")
  void shouldHandleExceptionDuringScheduledWarmup() {
    // Arrange
    doThrow(new RuntimeException("Service unavailable"))
        .when(searchApplicationService).getDistinctAuthors();

    // Act - Should not throw exception
    cacheWarmupService.scheduledCacheWarmup();

    // Assert
    verify(searchApplicationService).getDistinctAuthors();
  }

  @Test
  @DisplayName("Should evict all caches")
  void shouldEvictAllCaches() {
    // Arrange
    when(cacheManager.getCacheNames()).thenReturn(List.of("books", "book-search", "popular-books"));
    when(cacheManager.getCache("books")).thenReturn(bookCache);
    when(cacheManager.getCache("book-search")).thenReturn(searchCache);
    when(cacheManager.getCache("popular-books")).thenReturn(popularCache);

    // Act
    cacheWarmupService.evictAllCaches();

    // Assert
    verify(bookCache).clear();
    verify(searchCache).clear();
    verify(popularCache).clear();
  }

  @Test
  @DisplayName("Should handle null cache when evicting all caches")
  void shouldHandleNullCacheWhenEvictingAllCaches() {
    // Arrange
    when(cacheManager.getCacheNames()).thenReturn(List.of("books", "missing-cache"));
    when(cacheManager.getCache("books")).thenReturn(bookCache);
    when(cacheManager.getCache("missing-cache")).thenReturn(null);

    // Act
    cacheWarmupService.evictAllCaches();

    // Assert
    verify(bookCache).clear();
    verify(cacheManager).getCache("missing-cache");
  }

  @Test
  @DisplayName("Should evict book-related caches")
  void shouldEvictBookRelatedCaches() {
    // Arrange
    when(cacheManager.getCache(CacheConfig.BOOK_CACHE)).thenReturn(bookCache);
    when(cacheManager.getCache(CacheConfig.BOOK_SEARCH_CACHE)).thenReturn(searchCache);
    when(cacheManager.getCache(CacheConfig.POPULAR_BOOKS_CACHE)).thenReturn(popularCache);

    // Act
    cacheWarmupService.evictBookCaches();

    // Assert
    verify(bookCache).clear();
    verify(searchCache).clear();
    verify(popularCache).clear();
  }

  @Test
  @DisplayName("Should handle null caches when evicting book caches")
  void shouldHandleNullCachesWhenEvictingBookCaches() {
    // Arrange
    when(cacheManager.getCache(CacheConfig.BOOK_CACHE)).thenReturn(null);
    when(cacheManager.getCache(CacheConfig.BOOK_SEARCH_CACHE)).thenReturn(searchCache);
    when(cacheManager.getCache(CacheConfig.POPULAR_BOOKS_CACHE)).thenReturn(null);

    // Act
    cacheWarmupService.evictBookCaches();

    // Assert
    verify(searchCache).clear();
    verify(cacheManager).getCache(CacheConfig.BOOK_CACHE);
    verify(cacheManager).getCache(CacheConfig.POPULAR_BOOKS_CACHE);
  }

  @Test
  @DisplayName("Should warmup specific book cache")
  void shouldWarmupSpecificBookCache() {
    // Arrange
    var bookId = 123L;

    // Act
    cacheWarmupService.warmupSpecificBook(bookId);

    // Assert
    verify(catalogApplicationService).findBookById(bookId);
  }

  @Test
  @DisplayName("Should handle exception when warming up specific book")
  void shouldHandleExceptionWhenWarmingUpSpecificBook() {
    // Arrange
    var bookId = 999L;
    doThrow(new RuntimeException("Book not found"))
        .when(catalogApplicationService).findBookById(bookId);

    // Act - Should not throw exception
    cacheWarmupService.warmupSpecificBook(bookId);

    // Assert
    verify(catalogApplicationService).findBookById(bookId);
  }
}
