package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.gripday.bookstore.inventory.Inventory;
import org.gripday.bookstore.shared.ISBN;
import org.gripday.bookstore.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Book Domain Entity Tests")
class BookTest {

  private Category testCategory;

  @BeforeEach
  void setUp() {
    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setName("Fiction");
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create book with string parameters")
    void shouldCreateBookWithStringParameters() {
      // Act
      var book = Book.create(
          "Clean Code",
          "Robert C. Martin",
          "978-0132350884",
          new BigDecimal("45.99"),
          "A handbook of agile software craftsmanship",
          testCategory
      );

      // Assert
      assertThat(book).isNotNull();
      assertThat(book.getTitle()).isEqualTo("Clean Code");
      assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
      assertThat(book.getIsbn().getValue()).isEqualTo("9780132350884");
      assertThat(book.getPrice().getAmount()).isEqualByComparingTo("45.99");
      assertThat(book.getDescription()).isEqualTo("A handbook of agile software craftsmanship");
      assertThat(book.getCategory()).isEqualTo(testCategory);
      assertThat(book.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should create book with value objects")
    void shouldCreateBookWithValueObjects() {
      // Arrange
      var isbn = ISBN.of("978-0132350884");
      var price = Money.usd(new BigDecimal("45.99"));

      // Act
      var book = Book.create(
          "Clean Code",
          "Robert C. Martin",
          isbn,
          price,
          "A handbook of agile software craftsmanship",
          testCategory
      );

      // Assert
      assertThat(book).isNotNull();
      assertThat(book.getIsbn()).isEqualTo(isbn);
      assertThat(book.getPrice()).isEqualTo(price);
    }

    @Test
    @DisplayName("Should create book without description")
    void shouldCreateBookWithoutDescription() {
      // Act
      var book = Book.create(
          "Clean Code",
          "Robert C. Martin",
          "978-0132350884",
          new BigDecimal("45.99"),
          null,
          testCategory
      );

      // Assert
      assertThat(book.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should create book without category")
    void shouldCreateBookWithoutCategory() {
      // Act
      var book = Book.create(
          "Clean Code",
          "Robert C. Martin",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          null
      );

      // Assert
      assertThat(book.getCategory()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when title is null")
    void shouldThrowExceptionWhenTitleIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> Book.create(
          null,
          "Robert C. Martin",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          testCategory
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book title must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when title is blank")
    void shouldThrowExceptionWhenTitleIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> Book.create(
          "   ",
          "Robert C. Martin",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          testCategory
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book title must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when author is null")
    void shouldThrowExceptionWhenAuthorIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> Book.create(
          "Clean Code",
          null,
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          testCategory
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book author must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when author is blank")
    void shouldThrowExceptionWhenAuthorIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> Book.create(
          "Clean Code",
          "",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          testCategory
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book author must not be null or blank");
    }
  }

  @Nested
  @DisplayName("Update Details Tests")
  class UpdateDetailsTests {

    private Book book;

    @BeforeEach
    void setUp() {
      book = Book.create(
          "Original Title",
          "Original Author",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Original Description",
          testCategory
      );
    }

    @Test
    @DisplayName("Should update book details with Money")
    void shouldUpdateBookDetailsWithMoney() {
      // Arrange
      var newPrice = Money.usd(new BigDecimal("55.99"));

      // Act
      book.updateDetails(
          "Updated Title",
          "Updated Author",
          "Updated Description",
          newPrice
      );

      // Assert
      assertThat(book.getTitle()).isEqualTo("Updated Title");
      assertThat(book.getAuthor()).isEqualTo("Updated Author");
      assertThat(book.getDescription()).isEqualTo("Updated Description");
      assertThat(book.getPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("Should update book details with BigDecimal")
    void shouldUpdateBookDetailsWithBigDecimal() {
      // Act
      book.updateDetails(
          "Updated Title",
          "Updated Author",
          "Updated Description",
          new BigDecimal("55.99")
      );

      // Assert
      assertThat(book.getTitle()).isEqualTo("Updated Title");
      assertThat(book.getPrice().getAmount()).isEqualByComparingTo("55.99");
    }

    @Test
    @DisplayName("Should throw exception when updating with null title")
    void shouldThrowExceptionWhenUpdatingWithNullTitle() {
      // Act & Assert
      assertThatThrownBy(() -> book.updateDetails(
          null,
          "Author",
          "Description",
          Money.usd(new BigDecimal("45.99"))
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book title must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when updating with null author")
    void shouldThrowExceptionWhenUpdatingWithNullAuthor() {
      // Act & Assert
      assertThatThrownBy(() -> book.updateDetails(
          "Title",
          null,
          "Description",
          Money.usd(new BigDecimal("45.99"))
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book author must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when updating with null price")
    void shouldThrowExceptionWhenUpdatingWithNullPrice() {
      // Act & Assert
      assertThatThrownBy(() -> book.updateDetails(
          "Title",
          "Author",
          "Description",
          (Money) null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price must not be null");
    }
  }

  @Nested
  @DisplayName("Category Management Tests")
  class CategoryManagementTests {

    private Book book;
    private Category oldCategory;
    private Category newCategory;

    @BeforeEach
    void setUp() {
      oldCategory = mock(Category.class);
      newCategory = mock(Category.class);

      book = Book.create(
          "Test Book",
          "Test Author",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          null
      );
      book.setCategory(oldCategory);
    }

    @Test
    @DisplayName("Should change category and update relationships")
    void shouldChangeCategoryAndUpdateRelationships() {
      // Act
      book.changeCategory(newCategory);

      // Assert
      assertThat(book.getCategory()).isEqualTo(newCategory);
      verify(oldCategory).removeBook(book);
      verify(newCategory).addBook(book);
    }

    @Test
    @DisplayName("Should not change when new category is same as current")
    void shouldNotChangeWhenNewCategoryIsSameAsCurrent() {
      // Act - changing to the same category
      book.changeCategory(oldCategory);

      // Assert - category should remain the same
      assertThat(book.getCategory()).isEqualTo(oldCategory);
    }

    @Test
    @DisplayName("Should handle changing to null category")
    void shouldHandleChangingToNullCategory() {
      // Act
      book.changeCategory(null);

      // Assert
      assertThat(book.getCategory()).isNull();
      verify(oldCategory).removeBook(book);
    }

    @Test
    @DisplayName("Should handle changing from null category")
    void shouldHandleChangingFromNullCategory() {
      // Arrange
      book.setCategory(null);

      // Act
      book.changeCategory(newCategory);

      // Assert
      assertThat(book.getCategory()).isEqualTo(newCategory);
      verify(newCategory).addBook(book);
    }
  }

  @Nested
  @DisplayName("Availability Tests")
  class AvailabilityTests {

    private Book book;

    @BeforeEach
    void setUp() {
      book = Book.create(
          "Test Book",
          "Test Author",
          "978-0132350884",
          new BigDecimal("45.99"),
          "Description",
          testCategory
      );
    }

    @Test
    @DisplayName("Should mark book as unavailable")
    void shouldMarkBookAsUnavailable() {
      // Act
      book.markAsUnavailable();

      // Assert
      assertThat(book.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should mark book as available")
    void shouldMarkBookAsAvailable() {
      // Arrange
      book.markAsUnavailable();

      // Act
      book.markAsAvailable();

      // Assert
      assertThat(book.isAvailable()).isTrue();
    }
  }

  @Nested
  @DisplayName("Price Range Tests")
  class PriceRangeTests {

    private Book book;

    @BeforeEach
    void setUp() {
      book = Book.create(
          "Test Book",
          "Test Author",
          "978-0132350884",
          new BigDecimal("50.00"),
          "Description",
          testCategory
      );
    }

    @Test
    @DisplayName("Should return true when price is within range with Money")
    void shouldReturnTrueWhenPriceIsWithinRangeWithMoney() {
      // Arrange
      var minPrice = Money.usd(new BigDecimal("40.00"));
      var maxPrice = Money.usd(new BigDecimal("60.00"));

      // Act
      var result = book.isInPriceRange(minPrice, maxPrice);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when price equals min bound")
    void shouldReturnTrueWhenPriceEqualsMinBound() {
      // Arrange
      var minPrice = Money.usd(new BigDecimal("50.00"));
      var maxPrice = Money.usd(new BigDecimal("60.00"));

      // Act
      var result = book.isInPriceRange(minPrice, maxPrice);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when price equals max bound")
    void shouldReturnTrueWhenPriceEqualsMaxBound() {
      // Arrange
      var minPrice = Money.usd(new BigDecimal("40.00"));
      var maxPrice = Money.usd(new BigDecimal("50.00"));

      // Act
      var result = book.isInPriceRange(minPrice, maxPrice);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when price is below range")
    void shouldReturnFalseWhenPriceIsBelowRange() {
      // Arrange
      var minPrice = Money.usd(new BigDecimal("60.00"));
      var maxPrice = Money.usd(new BigDecimal("80.00"));

      // Act
      var result = book.isInPriceRange(minPrice, maxPrice);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when price is above range")
    void shouldReturnFalseWhenPriceIsAboveRange() {
      // Arrange
      var minPrice = Money.usd(new BigDecimal("20.00"));
      var maxPrice = Money.usd(new BigDecimal("40.00"));

      // Act
      var result = book.isInPriceRange(minPrice, maxPrice);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should work with BigDecimal parameters")
    void shouldWorkWithBigDecimalParameters() {
      // Act
      var result = book.isInPriceRange(new BigDecimal("40.00"), new BigDecimal("60.00"));

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when min price is null")
    void shouldThrowExceptionWhenMinPriceIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> book.isInPriceRange(null, Money.usd(new BigDecimal("60.00"))))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price range bounds must not be null");
    }

    @Test
    @DisplayName("Should throw exception when max price is null")
    void shouldThrowExceptionWhenMaxPriceIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> book.isInPriceRange(Money.usd(new BigDecimal("40.00")), null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price range bounds must not be null");
    }

    @Test
    @DisplayName("Should throw exception when BigDecimal min price is null")
    void shouldThrowExceptionWhenBigDecimalMinPriceIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> book.isInPriceRange(null, new BigDecimal("60.00")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price range bounds must not be null");
    }
  }

  @Nested
  @DisplayName("Affordability Tests")
  class AffordabilityTests {

    private Book book;

    @BeforeEach
    void setUp() {
      book = Book.create(
          "Test Book",
          "Test Author",
          "978-0132350884",
          new BigDecimal("50.00"),
          "Description",
          testCategory
      );
    }

    @Test
    @DisplayName("Should return true when book is affordable")
    void shouldReturnTrueWhenBookIsAffordable() {
      // Arrange
      var maxPrice = Money.usd(new BigDecimal("60.00"));

      // Act
      var result = book.isAffordable(maxPrice);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when price equals max price")
    void shouldReturnTrueWhenPriceEqualsMaxPrice() {
      // Arrange
      var maxPrice = Money.usd(new BigDecimal("50.00"));

      // Act
      var result = book.isAffordable(maxPrice);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when book is not affordable")
    void shouldReturnFalseWhenBookIsNotAffordable() {
      // Arrange
      var maxPrice = Money.usd(new BigDecimal("40.00"));

      // Act
      var result = book.isAffordable(maxPrice);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when max price is null")
    void shouldThrowExceptionWhenMaxPriceIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> book.isAffordable(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Max price must not be null");
    }
  }

  @Nested
  @DisplayName("Inventory Tests")
  class InventoryTests {

    private Book book;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
      book = Book.create(
          "Test Book",
          "Test Author",
          "978-0132350884",
          new BigDecimal("50.00"),
          "Description",
          testCategory
      );
      inventory = mock(Inventory.class);
    }

    @Test
    @DisplayName("Should return false when book has no inventory")
    void shouldReturnFalseWhenBookHasNoInventory() {
      // Act
      var result = book.hasInventory();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when book has inventory")
    void shouldReturnTrueWhenBookHasInventory() {
      // Arrange
      book.setInventory(inventory);

      // Act
      var result = book.hasInventory();

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when book is not in stock")
    void shouldReturnFalseWhenBookIsNotInStock() {
      // Act
      var result = book.isInStock();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when book is in stock")
    void shouldReturnTrueWhenBookIsInStock() {
      // Arrange
      book.setInventory(inventory);
      when(inventory.isAvailable()).thenReturn(true);

      // Act
      var result = book.isInStock();

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when inventory is not available")
    void shouldReturnFalseWhenInventoryIsNotAvailable() {
      // Arrange
      book.setInventory(inventory);
      when(inventory.isAvailable()).thenReturn(false);

      // Act
      var result = book.isInStock();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when book can be sold")
    void shouldReturnTrueWhenBookCanBeSold() {
      // Arrange
      book.setInventory(inventory);
      when(inventory.isAvailable()).thenReturn(true);

      // Act
      var result = book.canBeSold();

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when book is unavailable")
    void shouldReturnFalseWhenBookIsUnavailable() {
      // Arrange
      book.setInventory(inventory);
      book.markAsUnavailable();
      when(inventory.isAvailable()).thenReturn(true);

      // Act
      var result = book.canBeSold();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when book has no inventory for sale")
    void shouldReturnFalseWhenBookHasNoInventoryForSale() {
      // Act
      var result = book.canBeSold();

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return available quantity from inventory")
    void shouldReturnAvailableQuantityFromInventory() {
      // Arrange
      book.setInventory(inventory);
      when(inventory.getAvailableQuantity()).thenReturn(10);

      // Act
      var result = book.getAvailableQuantity();

      // Assert
      assertThat(result).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return zero when no inventory exists")
    void shouldReturnZeroWhenNoInventoryExists() {
      // Act
      var result = book.getAvailableQuantity();

      // Assert
      assertThat(result).isZero();
    }
  }

  @Nested
  @DisplayName("Setter Tests")
  class SetterTests {

    private Book book;

    @BeforeEach
    void setUp() {
      book = Book.create(
          "Test Book",
          "Test Author",
          "978-0132350884",
          new BigDecimal("50.00"),
          "Description",
          testCategory
      );
    }

    @Test
    @DisplayName("Should set ISBN with string")
    void shouldSetIsbnWithString() {
      // Act
      book.setIsbn("978-0134685991");

      // Assert
      assertThat(book.getIsbn().getValue()).isEqualTo("9780134685991");
    }

    @Test
    @DisplayName("Should set price with BigDecimal")
    void shouldSetPriceWithBigDecimal() {
      // Act
      book.setPrice(new BigDecimal("75.00"));

      // Assert
      assertThat(book.getPrice().getAmount()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Should set price with Money")
    void shouldSetPriceWithMoney() {
      // Arrange
      var newPrice = Money.usd(new BigDecimal("75.00"));

      // Act
      book.setPrice(newPrice);

      // Assert
      assertThat(book.getPrice()).isEqualTo(newPrice);
    }
  }
}
