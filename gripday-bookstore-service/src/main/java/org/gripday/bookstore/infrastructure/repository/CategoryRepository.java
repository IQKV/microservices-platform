package org.gripday.bookstore.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.gripday.bookstore.infrastructure.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  // Basic CRUD operations are inherited from JpaRepository

  // Find by name (case-insensitive)
  Optional<Category> findByNameIgnoreCase(String name);

  // Check if category exists by name
  boolean existsByNameIgnoreCase(String name);

  // Find categories with books
  @Query("SELECT DISTINCT c FROM Category c JOIN c.books b WHERE b.available = true")
  List<Category> findCategoriesWithAvailableBooks();

  // Find categories by partial name match
  List<Category> findByNameContainingIgnoreCase(String nameFragment);

  // Get category statistics
  @Query("""
      SELECT c FROM Category c 
      LEFT JOIN c.books b 
      GROUP BY c.id, c.name, c.description, c.createdAt, c.updatedAt
      HAVING COUNT(b) > 0
      ORDER BY COUNT(b) DESC
      """)
  List<Category> findCategoriesOrderedByBookCount();

  // Find empty categories (no books)
  @Query("SELECT c FROM Category c WHERE c.books IS EMPTY")
  List<Category> findEmptyCategories();

  // Count books in category
  @Query("SELECT COUNT(b) FROM Category c JOIN c.books b WHERE c.id = :categoryId AND b.available = true")
  long countAvailableBooksInCategory(@Param("categoryId") Long categoryId);

  // Find categories with low stock books
  @Query("""
      SELECT DISTINCT c FROM Category c 
      JOIN c.books b 
      JOIN b.inventory i 
      WHERE i.quantity <= i.lowStockThreshold 
      AND b.available = true
      """)
  List<Category> findCategoriesWithLowStockBooks();

  // Get all categories ordered by name
  List<Category> findAllByOrderByNameAsc();

  // Find categories created within a date range (useful for admin reporting)
  @Query("SELECT c FROM Category c WHERE c.createdAt >= :startDate ORDER BY c.createdAt DESC")
  List<Category> findRecentCategories(@Param("startDate") java.time.LocalDateTime startDate);
}