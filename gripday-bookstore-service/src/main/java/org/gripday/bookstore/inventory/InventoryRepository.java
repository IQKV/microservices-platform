package org.gripday.bookstore.inventory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Optional<Inventory> findByBookId(Long bookId);

  @Query("SELECT i FROM Inventory i WHERE i.book.isbn = :isbn")
  Optional<Inventory> findByBookIsbn(@Param("isbn") String isbn);

  @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.lowStockThreshold")
  List<Inventory> findLowStockInventory();

  @Query("SELECT i FROM Inventory i WHERE i.quantity = 0")
  List<Inventory> findOutOfStockInventory();

  @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 AND i.book.available = true")
  List<Inventory> findAvailableInventory();

  @Query("SELECT i FROM Inventory i WHERE i.reservedQuantity > 0")
  List<Inventory> findInventoryWithReservations();

  @Query("SELECT SUM(i.reservedQuantity) FROM Inventory i WHERE i.book.id = :bookId")
  Integer getTotalReservedQuantityForBook(@Param("bookId") Long bookId);

  @Query("SELECT i FROM Inventory i WHERE i.book.id IN :bookIds")
  List<Inventory> findByBookIds(@Param("bookIds") List<Long> bookIds);

  @Modifying
  @Query("UPDATE Inventory i SET i.quantity = :quantity WHERE i.book.id = :bookId")
  int updateQuantityByBookId(@Param("bookId") Long bookId, @Param("quantity") int quantity);

  @Modifying
  @Query("UPDATE Inventory i SET i.reservedQuantity = :reservedQuantity WHERE i.book.id = :bookId")
  int updateReservedQuantityByBookId(@Param("bookId") Long bookId, @Param("reservedQuantity") int reservedQuantity);

  @Modifying
  @Query("UPDATE Inventory i SET i.lowStockThreshold = :threshold WHERE i.book.id = :bookId")
  int updateLowStockThresholdByBookId(@Param("bookId") Long bookId, @Param("threshold") int threshold);

  @Modifying
  @Query("""
      UPDATE Inventory i 
      SET i.quantity = i.quantity + :adjustment 
      WHERE i.book.id IN :bookIds 
      AND (i.quantity + :adjustment) >= 0
      """)
  int bulkAdjustQuantity(@Param("bookIds") List<Long> bookIds, @Param("adjustment") int adjustment);

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

  @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i")
  Long sumTotalQuantity();

  @Query("""
      SELECT COUNT(i) FROM Inventory i 
      WHERE i.quantity <= i.lowStockThreshold 
      AND i.book.available = true
      """)
  long countLowStockBooks();

  @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity = 0 AND i.book.available = true")
  long countOutOfStockBooks();

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

  @Query("""
      SELECT CASE WHEN (i.quantity - i.reservedQuantity) >= :requestedQuantity 
      THEN true ELSE false END 
      FROM Inventory i 
      WHERE i.book.id = :bookId
      """)
  Boolean isQuantityAvailable(@Param("bookId") Long bookId, @Param("requestedQuantity") int requestedQuantity);

  @Query("SELECT b.id FROM Book b WHERE b.inventory IS NULL AND b.available = true")
  List<Long> findBookIdsWithoutInventory();

  @Query("SELECT i FROM Inventory i WHERE i.updatedAt >= :since ORDER BY i.updatedAt DESC")
  List<Inventory> findRecentlyUpdatedInventory(@Param("since") java.time.LocalDateTime since);

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
