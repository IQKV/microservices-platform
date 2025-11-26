package com.iqscaffold.bookstore.catalog;

import java.util.List;
import java.util.Optional;

import com.iqscaffold.bookstore.shared.AuditLogger;
import com.iqscaffold.bookstore.shared.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Category use cases.
 * Orchestrates domain logic, manages transactions, and handles DTO conversions.
 */
@Service
@Transactional
public class CategoryApplicationService {

  private static final Logger logger = LoggerFactory.getLogger(CategoryApplicationService.class);

  private final CategoryRepository categoryRepository;
  private final AuditLogger auditLogger;

  public CategoryApplicationService(
      final CategoryRepository categoryRepository,
      final AuditLogger auditLogger) {
    this.categoryRepository = categoryRepository;
    this.auditLogger = auditLogger;
  }

  @Transactional(readOnly = true)
  public List<CategoryDto> findAllCategories() {
    logger.debug("Finding all categories");
    return categoryRepository.findAllByOrderByNameAsc()
        .stream()
        .map(this::convertToDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<CategoryDto> findCategoryById(Long id) {
    logger.debug("Finding category by ID: {}", id);
    return categoryRepository.findById(id)
        .map(this::convertToDto);
  }

  @Transactional(readOnly = true)
  public Optional<CategoryDto> findCategoryByName(String name) {
    logger.debug("Finding category by name: {}", name);
    return categoryRepository.findByNameIgnoreCase(name)
        .map(this::convertToDto);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  public CategoryDto createCategory(String name, String description, UserContext userContext) {
    logger.info("Creating category with name: {} by user: {}", name, userContext.username());

    if (categoryRepository.existsByNameIgnoreCase(name)) {
      throw new DuplicateCategoryException(name);
    }

    var category = Category.create(name, description);
    var savedCategory = categoryRepository.save(category);

    auditLogger.logCategoryCreation(savedCategory.getId(), savedCategory.getName(), userContext);

    logger.info("Successfully created category with ID: {} by user: {}", savedCategory.getId(), userContext.username());

    return convertToDto(savedCategory);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  public CategoryDto updateCategory(Long id, String name, String description, UserContext userContext) {
    logger.info("Updating category ID: {} by user: {}", id, userContext.username());

    var category = categoryRepository.findById(id)
        .orElseThrow(() -> new CategoryNotFoundException(id));

    if (!category.getName().equalsIgnoreCase(name)
        && categoryRepository.existsByNameIgnoreCase(name)) {
      throw new DuplicateCategoryException(name);
    }

    category.setName(name);
    category.setDescription(description);

    var updatedCategory = categoryRepository.save(category);

    auditLogger.logCategoryUpdate(updatedCategory.getId(), updatedCategory.getName(), userContext);

    logger.info("Successfully updated category ID: {} by user: {}", id, userContext.username());

    return convertToDto(updatedCategory);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  public void deleteCategory(Long id, UserContext userContext) {
    logger.info("Deleting category ID: {} by user: {}", id, userContext.username());

    var category = categoryRepository.findById(id)
        .orElseThrow(() -> new CategoryNotFoundException(id));

    var bookCount = category.getBooks().size();
    if (bookCount > 0) {
      throw new CategoryInUseException(id, category.getName(), bookCount);
    }

    categoryRepository.delete(category);

    auditLogger.logCategoryDeletion(category.getId(), category.getName(), userContext);

    logger.info("Successfully deleted category ID: {} by user: {}", id, userContext.username());
  }

  @Transactional(readOnly = true)
  public List<CategoryDto> findEmptyCategories() {
    logger.debug("Finding empty categories");
    return categoryRepository.findEmptyCategories()
        .stream()
        .map(this::convertToDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public long countAvailableBooksInCategory(Long categoryId) {
    logger.debug("Counting available books in category ID: {}", categoryId);
    return categoryRepository.countAvailableBooksInCategory(categoryId);
  }

  private CategoryDto convertToDto(Category category) {
    return new CategoryDto(
        category.getId(),
        category.getName(),
        category.getDescription(),
        category.getBooks().size(),
        category.getCreatedAt(),
        category.getUpdatedAt()
    );
  }
}
