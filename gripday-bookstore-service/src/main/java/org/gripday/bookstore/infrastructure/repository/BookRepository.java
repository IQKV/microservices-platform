package org.gripday.bookstore.infrastructure.repository;

import org.gripday.bookstore.infrastructure.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Basic search methods by individual fields
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);
    
    Page<Book> findByCategoryName(String categoryName, Pageable pageable);
    
    Page<Book> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Availability filtering
    @Query("SELECT b FROM Book b WHERE b.available = true")
    Page<Book> findAvailableBooks(Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.available = true AND b.inventory.quantity > 0")
    Page<Book> findBooksInStock(Pageable pageable);
    
    // Optimized combined search methods for multiple criteria
    @Query("""
        SELECT b FROM Book b 
        WHERE (:availableOnly = false OR b.available = true)
        AND (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')))
        AND (:categoryName IS NULL OR b.category.name = :categoryName)
        AND (:minPrice IS NULL OR b.price >= :minPrice)
        AND (:maxPrice IS NULL OR b.price <= :maxPrice)
        ORDER BY b.available DESC, b.createdAt DESC
        """)
    Page<Book> findBooksWithCriteria(
        @Param("title") String title,
        @Param("author") String author,
        @Param("categoryName") String categoryName,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("availableOnly") boolean availableOnly,
        Pageable pageable
    );
    
    // Optimized advanced search with inventory considerations
    @Query("""
        SELECT b FROM Book b 
        LEFT JOIN FETCH b.inventory i
        LEFT JOIN FETCH b.category c
        WHERE (:availableOnly = false OR b.available = true)
        AND (:availableOnly = false OR i.quantity > 0)
        AND (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')))
        AND (:categoryName IS NULL OR c.name = :categoryName)
        AND (:minPrice IS NULL OR b.price >= :minPrice)
        AND (:maxPrice IS NULL OR b.price <= :maxPrice)
        ORDER BY b.available DESC, i.quantity DESC, b.createdAt DESC
        """)
    Page<Book> findBooksWithInventoryFilter(
        @Param("title") String title,
        @Param("author") String author,
        @Param("categoryName") String categoryName,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("availableOnly") boolean availableOnly,
        Pageable pageable
    );
    
    // ISBN lookup
    Optional<Book> findByIsbn(String isbn);
    
    // Category-based queries
    @Query("SELECT b FROM Book b WHERE b.category.id = :categoryId AND b.available = true")
    Page<Book> findAvailableBooksByCategory(@Param("categoryId") Long categoryId, Pageable pageable);
    
    // Price range queries
    @Query("SELECT b FROM Book b WHERE b.price <= :maxPrice AND b.available = true ORDER BY b.price ASC")
    Page<Book> findAffordableBooks(@Param("maxPrice") BigDecimal maxPrice, Pageable pageable);
    
    // Popular/featured books (can be extended with rating/sales data later)
    @Query("SELECT b FROM Book b WHERE b.available = true ORDER BY b.createdAt DESC")
    Page<Book> findRecentBooks(Pageable pageable);
    
    // Low stock books for admin management
    @Query("""
        SELECT b FROM Book b 
        JOIN b.inventory i 
        WHERE i.quantity <= i.lowStockThreshold 
        AND b.available = true
        """)
    List<Book> findLowStockBooks();
    
    // Books without inventory records
    @Query("SELECT b FROM Book b WHERE b.inventory IS NULL")
    List<Book> findBooksWithoutInventory();
    
    // Count queries for statistics
    @Query("SELECT COUNT(b) FROM Book b WHERE b.available = true")
    long countAvailableBooks();
    
    long countByAvailableTrue();
    
    @Query("SELECT COUNT(b) FROM Book b WHERE b.category.name = :categoryName AND b.available = true")
    long countBooksByCategory(@Param("categoryName") String categoryName);
    
    // Optimized distinct authors and categories for filter options
    @Query("SELECT DISTINCT b.author FROM Book b WHERE b.available = true ORDER BY b.author")
    List<String> findDistinctAuthors();
    
    @Query("SELECT DISTINCT c.name FROM Book b JOIN b.category c WHERE b.available = true ORDER BY c.name")
    List<String> findDistinctCategoryNames();
    
    // Fast text search using PostgreSQL full-text search capabilities
    @Query(value = """
        SELECT * FROM books b 
        WHERE b.available = true 
        AND to_tsvector('english', b.title || ' ' || b.author) @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(to_tsvector('english', b.title || ' ' || b.author), plainto_tsquery('english', :searchTerm)) DESC
        """, nativeQuery = true)
    Page<Book> findByFullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Fuzzy search using trigram similarity
    @Query(value = """
        SELECT * FROM books b 
        WHERE b.available = true 
        AND (similarity(b.title, :searchTerm) > 0.3 OR similarity(b.author, :searchTerm) > 0.3)
        ORDER BY GREATEST(similarity(b.title, :searchTerm), similarity(b.author, :searchTerm)) DESC
        """, nativeQuery = true)
    Page<Book> findByFuzzySearch(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Optimized price range query with category filter
    @Query("""
        SELECT b FROM Book b 
        WHERE b.available = true 
        AND b.price BETWEEN :minPrice AND :maxPrice
        AND (:categoryId IS NULL OR b.category.id = :categoryId)
        ORDER BY b.price ASC
        """)
    Page<Book> findByPriceRangeAndCategory(
        @Param("minPrice") BigDecimal minPrice, 
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );
    
    // Fast availability check with inventory
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END 
        FROM Book b JOIN b.inventory i 
        WHERE b.id = :bookId AND b.available = true AND i.quantity > :requestedQuantity
        """)
    boolean isBookAvailableWithQuantity(@Param("bookId") Long bookId, @Param("requestedQuantity") int requestedQuantity);
}