package org.gripday.bookstore.infrastructure.repository;

import org.gripday.bookstore.infrastructure.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    // Find inventory by book ID
    Optional<Inventory> findByBookId(Long bookId);
    
    // Find inventory by book ISBN
    @Query("SELECT i FROM Inventory i WHERE i.book.isbn = :isbn")
    Optional<Inventory> findByBookIsbn(@Param("isbn") String isbn);
    
    // Stock management queries
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.lowStockThreshold")
    List<Inventory> findLowStockInventory();
    
    @Query("SELECT i FROM Inventory i WHERE i.quantity = 0")
    List<Inventory> findOutOfStockInventory();
    
    @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 AND i.book.available = true")
    List<Inventory> findAvailableInventory();
    
    // Reserved quantity management
    @Query("SELECT i FROM Inventory i WHERE i.reservedQuantity > 0")
    List<Inventory> findInventoryWithReservations();
    
    @Query("SELECT SUM(i.reservedQuantity) FROM Inventory i WHERE i.book.id = :bookId")
    Integer getTotalReservedQuantityForBook(@Param("bookId") Long bookId);
    
    // Bulk operations support
    @Query("SELECT i FROM Inventory i WHERE i.book.id IN :bookIds")
    List<Inventory> findByBookIds(@Param("bookIds") List<Long> bookIds);
    
    // Update operations
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = :quantity WHERE i.book.id = :bookId")
    int updateQuantityByBookId(@Param("bookId") Long bookId, @Param("quantity") int quantity);
    
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = :reservedQuantity WHERE i.book.id = :bookId")
    int updateReservedQuantityByBookId(@Param("bookId") Long bookId, @Param("reservedQuantity") int reservedQuantity);
    
    @Modifying
    @Query("UPDATE Inventory i SET i.lowStockThreshold = :threshold WHERE i.book.id = :bookId")
    int updateLowStockThresholdByBookId(@Param("bookId") Long bookId, @Param("threshold") int threshold);
    
    // Bulk quantity adjustments
    @Modifying
    @Query("""
        UPDATE Inventory i 
        SET i.quantity = i.quantity + :adjustment 
        WHERE i.book.id IN :bookIds 
        AND (i.quantity + :adjustment) >= 0
        """)
    int bulkAdjustQuantity(@Param("bookIds") List<Long> bookIds, @Param("adjustment") int adjustment);
    
    // Statistics and reporting
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.book.available = true")
    Long getTotalInventoryCount();
    
    @Query("SELECT SUM(i.reservedQuantity) FROM Inventory i WHERE i.book.available = true")
    Long getTotalReservedCount();
    
    @Query("""
        SELECT COUNT(i) FROM Inventory i 
        WHERE i.quantity <= i.lowStockThreshold 
        AND i.book.available = true
        """)
    long countLowStockItems();
    
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity = 0 AND i.book.available = true")
    long countOutOfStockItems();
    
    // Category-based inventory queries
    @Query("""
        SELECT i FROM Inventory i 
        WHERE i.book.category.name = :categoryName 
        AND i.quantity <= i.lowStockThreshold
        """)
    List<Inventory> findLowStockInventoryByCategory(@Param("categoryName") String categoryName);
    
    @Query("""
        SELECT SUM(i.quantity) FROM Inventory i 
        WHERE i.book.category.name = :categoryName 
        AND i.book.available = true
        """)
    Long getTotalInventoryCountByCategory(@Param("categoryName") String categoryName);
    
    // Availability checks
    @Query("""
        SELECT CASE WHEN (i.quantity - i.reservedQuantity) >= :requestedQuantity 
        THEN true ELSE false END 
        FROM Inventory i 
        WHERE i.book.id = :bookId
        """)
    Boolean isQuantityAvailable(@Param("bookId") Long bookId, @Param("requestedQuantity") int requestedQuantity);
    
    // Find books that need inventory records
    @Query("SELECT b.id FROM Book b WHERE b.inventory IS NULL AND b.available = true")
    List<Long> findBookIdsWithoutInventory();
    
    // Inventory history and audit support (for future expansion)
    @Query("SELECT i FROM Inventory i WHERE i.updatedAt >= :since ORDER BY i.updatedAt DESC")
    List<Inventory> findRecentlyUpdatedInventory(@Param("since") java.time.LocalDateTime since);
    
    // Advanced stock management
    @Query("""
        SELECT i FROM Inventory i 
        WHERE (i.quantity - i.reservedQuantity) BETWEEN :minAvailable AND :maxAvailable
        AND i.book.available = true
        """)
    List<Inventory> findInventoryByAvailableQuantityRange(
        @Param("minAvailable") int minAvailable, 
        @Param("maxAvailable") int maxAvailable
    );
}