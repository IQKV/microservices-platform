package org.gripday.bookstore.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  Optional<Category> findByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);

  @Query("SELECT DISTINCT c FROM Category c JOIN c.books b WHERE b.available = true")
  List<Category> findCategoriesWithAvailableBooks();

  List<Category> findByNameContainingIgnoreCase(String nameFragment);

  @Query("""
      SELECT c FROM Category c 
      LEFT JOIN c.books b 
      GROUP BY c.id, c.name, c.description, c.createdAt, c.updatedAt
      HAVING COUNT(b) > 0
      ORDER BY COUNT(b) DESC
      """)
  List<Category> findCategoriesOrderedByBookCount();

  @Query("SELECT c FROM Category c WHERE c.books IS EMPTY")
  List<Category> findEmptyCategories();

  @Query("SELECT COUNT(b) FROM Category c JOIN c.books b WHERE c.id = :categoryId AND b.available = true")
  long countAvailableBooksInCategory(@Param("categoryId") Long categoryId);

  @Query("""
      SELECT DISTINCT c FROM Category c 
      JOIN c.books b 
      JOIN b.inventory i 
      WHERE i.quantity <= i.lowStockThreshold 
      AND b.available = true
      """)
  List<Category> findCategoriesWithLowStockBooks();

  List<Category> findAllByOrderByNameAsc();

  @Query("SELECT c FROM Category c WHERE c.createdAt >= :startDate ORDER BY c.createdAt DESC")
  List<Category> findRecentCategories(@Param("startDate") java.time.LocalDateTime startDate);
}
