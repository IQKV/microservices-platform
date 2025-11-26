package com.iqscaffold.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.iqscaffold.bookstore.shared.AuditLogger;
import com.iqscaffold.bookstore.shared.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryApplicationService Tests")
class CategoryApplicationServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private AuditLogger auditLogger;

  @InjectMocks
  private CategoryApplicationService categoryApplicationService;

  private UserContext testUserContext;
  private Category testCategory;

  @BeforeEach
  void setUp() {
    testUserContext = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("ADMIN"),
        Set.of(),
        "Engineering",
        "1",
        Map.of()
    );

    testCategory = Category.create("Fiction", "Fiction books");
    testCategory.setId(1L);
    testCategory.setCreatedAt(LocalDateTime.now());
    testCategory.setUpdatedAt(LocalDateTime.now());
  }

  @Nested
  @DisplayName("Find Category Tests")
  class FindCategoryTests {

    @Test
    @DisplayName("Should find all categories ordered by name")
    void shouldFindAllCategoriesOrderedByName() {
      // Arrange
      var categories = List.of(testCategory);
      when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(categories);

      // Act
      var result = categoryApplicationService.findAllCategories();

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.get(0).name()).isEqualTo("Fiction");
      verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("Should find category by ID")
    void shouldFindCategoryById() {
      // Arrange
      when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

      // Act
      var result = categoryApplicationService.findCategoryById(1L);

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().name()).isEqualTo("Fiction");
      verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when category not found by ID")
    void shouldReturnEmptyWhenCategoryNotFoundById() {
      // Arrange
      when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      var result = categoryApplicationService.findCategoryById(999L);

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find category by name")
    void shouldFindCategoryByName() {
      // Arrange
      when(categoryRepository.findByNameIgnoreCase("Fiction")).thenReturn(Optional.of(testCategory));

      // Act
      var result = categoryApplicationService.findCategoryByName("Fiction");

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().name()).isEqualTo("Fiction");
    }
  }

  @Nested
  @DisplayName("Create Category Tests")
  class CreateCategoryTests {

    @Test
    @DisplayName("Should create category successfully")
    void shouldCreateCategorySuccessfully() {
      // Arrange
      when(categoryRepository.existsByNameIgnoreCase("Science")).thenReturn(false);
      when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

      // Act
      var result = categoryApplicationService.createCategory("Science", "Science books", testUserContext);

      // Assert
      assertThat(result).isNotNull();
      verify(categoryRepository).existsByNameIgnoreCase("Science");
      verify(categoryRepository).save(any(Category.class));
      verify(auditLogger).logCategoryCreation(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when category name already exists")
    void shouldThrowExceptionWhenCategoryNameAlreadyExists() {
      // Arrange
      when(categoryRepository.existsByNameIgnoreCase("Fiction")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() ->
          categoryApplicationService.createCategory("Fiction", "Fiction books", testUserContext))
          .isInstanceOf(DuplicateCategoryException.class)
          .hasMessageContaining("Fiction");

      verify(categoryRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Update Category Tests")
  class UpdateCategoryTests {

    @Test
    @DisplayName("Should update category successfully")
    void shouldUpdateCategorySuccessfully() {
      // Arrange
      when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
      when(categoryRepository.existsByNameIgnoreCase("Updated Fiction")).thenReturn(false);
      when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

      // Act
      var result = categoryApplicationService.updateCategory(
          1L, "Updated Fiction", "Updated description", testUserContext);

      // Assert
      assertThat(result).isNotNull();
      verify(categoryRepository).save(any(Category.class));
      verify(auditLogger).logCategoryUpdate(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
      // Arrange
      when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() ->
          categoryApplicationService.updateCategory(999L, "New Name", "Description", testUserContext))
          .isInstanceOf(CategoryNotFoundException.class);

      verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when new name already exists")
    void shouldThrowExceptionWhenNewNameAlreadyExists() {
      // Arrange
      when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
      when(categoryRepository.existsByNameIgnoreCase("Science")).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() ->
          categoryApplicationService.updateCategory(1L, "Science", "Description", testUserContext))
          .isInstanceOf(DuplicateCategoryException.class);

      verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow updating with same name (case insensitive)")
    void shouldAllowUpdatingWithSameName() {
      // Arrange
      when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
      when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

      // Act
      var result = categoryApplicationService.updateCategory(
          1L, "fiction", "Updated description", testUserContext);

      // Assert
      assertThat(result).isNotNull();
      verify(categoryRepository).save(any(Category.class));
    }
  }

  @Nested
  @DisplayName("Delete Category Tests")
  class DeleteCategoryTests {

    @Test
    @DisplayName("Should delete empty category successfully")
    void shouldDeleteEmptyCategorySuccessfully() {
      // Arrange
      testCategory.setBooks(new ArrayList<>());
      when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

      // Act
      categoryApplicationService.deleteCategory(1L, testUserContext);

      // Assert
      verify(categoryRepository).delete(testCategory);
      verify(auditLogger).logCategoryDeletion(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
      // Arrange
      when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() ->
          categoryApplicationService.deleteCategory(999L, testUserContext))
          .isInstanceOf(CategoryNotFoundException.class);

      verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when category has books")
    void shouldThrowExceptionWhenCategoryHasBooks() {
      // Arrange
      var books = new ArrayList<Book>();
      books.add(new Book());
      books.add(new Book());
      testCategory.setBooks(books);
      when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

      // Act & Assert
      assertThatThrownBy(() ->
          categoryApplicationService.deleteCategory(1L, testUserContext))
          .isInstanceOf(CategoryInUseException.class)
          .hasMessageContaining("2 book(s)");

      verify(categoryRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("Utility Tests")
  class UtilityTests {

    @Test
    @DisplayName("Should find empty categories")
    void shouldFindEmptyCategories() {
      // Arrange
      var emptyCategories = List.of(testCategory);
      when(categoryRepository.findEmptyCategories()).thenReturn(emptyCategories);

      // Act
      var result = categoryApplicationService.findEmptyCategories();

      // Assert
      assertThat(result).hasSize(1);
      verify(categoryRepository).findEmptyCategories();
    }

    @Test
    @DisplayName("Should count available books in category")
    void shouldCountAvailableBooksInCategory() {
      // Arrange
      when(categoryRepository.countAvailableBooksInCategory(1L)).thenReturn(5L);

      // Act
      var result = categoryApplicationService.countAvailableBooksInCategory(1L);

      // Assert
      assertThat(result).isEqualTo(5L);
      verify(categoryRepository).countAvailableBooksInCategory(1L);
    }
  }
}
