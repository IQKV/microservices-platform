package org.gripday.bookstore.domain.service;

import org.gripday.bookstore.infrastructure.config.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheWarmupService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);
    
    private final SearchService searchService;
    private final BookService bookService;
    private final CacheManager cacheManager;
    
    public CacheWarmupService(SearchService searchService, BookService bookService, CacheManager cacheManager) {
        this.searchService = searchService;
        this.bookService = bookService;
        this.cacheManager = cacheManager;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    @Async("cacheWarmupExecutor")
    public void warmupCacheOnStartup() {
        logger.info("Starting cache warmup process...");
        
        try {
            // Warm up popular book listings
            warmupPopularBooks();
            
            // Warm up filter options
            warmupFilterOptions();
            
            logger.info("Cache warmup completed successfully");
        } catch (Exception e) {
            logger.error("Cache warmup failed", e);
        }
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Async("cacheWarmupExecutor")
    public void scheduledCacheWarmup() {
        logger.debug("Running scheduled cache warmup...");
        
        try {
            warmupPopularBooks();
            warmupFilterOptions();
        } catch (Exception e) {
            logger.warn("Scheduled cache warmup failed", e);
        }
    }
    
    private void warmupPopularBooks() {
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        
        // Warm up available books (most commonly accessed)
        bookService.findAvailableBooks(pageable);
        logger.debug("Warmed up available books cache");
        
        // Warm up books in stock
        bookService.findBooksInStock(pageable);
        logger.debug("Warmed up books in stock cache");
        
        // Warm up recent books
        searchService.findRecentBooks(pageable);
        logger.debug("Warmed up recent books cache");
    }
    
    private void warmupFilterOptions() {
        // Warm up distinct authors and categories for filter dropdowns
        searchService.getDistinctAuthors();
        logger.debug("Warmed up distinct authors cache");
        
        searchService.getDistinctCategories();
        logger.debug("Warmed up distinct categories cache");
    }
    
    public void evictAllCaches() {
        logger.info("Evicting all caches...");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            }
        });
        
        logger.info("All caches evicted");
    }
    
    public void evictBookCaches() {
        logger.info("Evicting book-related caches...");
        
        var bookCache = cacheManager.getCache(CacheConfiguration.BOOK_CACHE);
        if (bookCache != null) {
            bookCache.clear();
            logger.debug("Cleared book cache");
        }
        
        var searchCache = cacheManager.getCache(CacheConfiguration.BOOK_SEARCH_CACHE);
        if (searchCache != null) {
            searchCache.clear();
            logger.debug("Cleared book search cache");
        }
        
        var popularCache = cacheManager.getCache(CacheConfiguration.POPULAR_BOOKS_CACHE);
        if (popularCache != null) {
            popularCache.clear();
            logger.debug("Cleared popular books cache");
        }
        
        logger.info("Book-related caches evicted");
    }
    
    public void warmupSpecificBook(Long bookId) {
        try {
            bookService.findBookById(bookId);
            logger.debug("Warmed up cache for book ID: {}", bookId);
        } catch (Exception e) {
            logger.warn("Failed to warm up cache for book ID: {}", bookId, e);
        }
    }
}