package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money Value Object Tests")
class MoneyTest {

  @Test
  @DisplayName("Should create Money with valid amount and currency")
  void shouldCreateMoneyWithValidAmountAndCurrency() {
    // Arrange & Act
    var money = Money.of(new BigDecimal("100.00"), "USD");

    // Assert
    assertThat(money.getAmount()).isEqualByComparingTo("100.00");
    assertThat(money.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should create Money with USD currency")
  void shouldCreateMoneyWithUsd() {
    // Arrange & Act
    var money = Money.usd(new BigDecimal("50.00"));

    // Assert
    assertThat(money.getAmount()).isEqualByComparingTo("50.00");
    assertThat(money.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should create Money with EUR currency")
  void shouldCreateMoneyWithEur() {
    // Arrange & Act
    var money = Money.eur(new BigDecimal("75.00"));

    // Assert
    assertThat(money.getAmount()).isEqualByComparingTo("75.00");
    assertThat(money.getCurrency()).isEqualTo("EUR");
  }

  @Test
  @DisplayName("Should create zero Money")
  void shouldCreateZeroMoney() {
    // Arrange & Act
    var money = Money.zero("USD");

    // Assert
    assertThat(money.getAmount()).isEqualByComparingTo("0.00");
    assertThat(money.isZero()).isTrue();
    assertThat(money.isPositive()).isFalse();
  }

  @Test
  @DisplayName("Should round amount to 2 decimal places")
  void shouldRoundAmountToTwoDecimalPlaces() {
    // Arrange & Act
    var money = Money.of(new BigDecimal("100.456"), "USD");

    // Assert
    assertThat(money.getAmount()).isEqualByComparingTo("100.46");
  }

  @Test
  @DisplayName("Should convert currency to uppercase")
  void shouldConvertCurrencyToUppercase() {
    // Arrange & Act
    var money = Money.of(new BigDecimal("100.00"), "usd");

    // Assert
    assertThat(money.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should throw exception for null amount")
  void shouldThrowExceptionForNullAmount() {
    // Arrange, Act & Assert
    assertThatThrownBy(() -> Money.of(null, "USD"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Amount must not be null");
  }

  @Test
  @DisplayName("Should throw exception for negative amount")
  void shouldThrowExceptionForNegativeAmount() {
    // Arrange, Act & Assert
    assertThatThrownBy(() -> Money.of(new BigDecimal("-10.00"), "USD"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Amount must not be negative");
  }

  @Test
  @DisplayName("Should throw exception for null currency")
  void shouldThrowExceptionForNullCurrency() {
    // Arrange, Act & Assert
    assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Currency must not be null or blank");
  }

  @Test
  @DisplayName("Should throw exception for blank currency")
  void shouldThrowExceptionForBlankCurrency() {
    // Arrange, Act & Assert
    assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Currency must not be null or blank");
  }

  @Test
  @DisplayName("Should throw exception for invalid currency code")
  void shouldThrowExceptionForInvalidCurrencyCode() {
    // Arrange, Act & Assert
    assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), "INVALID"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid currency code");
  }

  @Test
  @DisplayName("Should add two Money values with same currency")
  void shouldAddTwoMoneyValuesWithSameCurrency() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.usd(new BigDecimal("50.00"));

    // Act
    var result = money1.add(money2);

    // Assert
    assertThat(result.getAmount()).isEqualByComparingTo("150.00");
    assertThat(result.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should throw exception when adding Money with different currencies")
  void shouldThrowExceptionWhenAddingMoneyWithDifferentCurrencies() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.eur(new BigDecimal("50.00"));

    // Act & Assert
    assertThatThrownBy(() -> money1.add(money2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Currency mismatch");
  }

  @Test
  @DisplayName("Should subtract two Money values with same currency")
  void shouldSubtractTwoMoneyValuesWithSameCurrency() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.usd(new BigDecimal("30.00"));

    // Act
    var result = money1.subtract(money2);

    // Assert
    assertThat(result.getAmount()).isEqualByComparingTo("70.00");
    assertThat(result.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should throw exception when subtracting results in negative")
  void shouldThrowExceptionWhenSubtractingResultsInNegative() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("50.00"));
    var money2 = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThatThrownBy(() -> money1.subtract(money2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Subtraction would result in negative amount");
  }

  @Test
  @DisplayName("Should throw exception when subtracting Money with different currencies")
  void shouldThrowExceptionWhenSubtractingMoneyWithDifferentCurrencies() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.eur(new BigDecimal("50.00"));

    // Act & Assert
    assertThatThrownBy(() -> money1.subtract(money2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Currency mismatch");
  }

  @Test
  @DisplayName("Should multiply Money by BigDecimal factor")
  void shouldMultiplyMoneyByBigDecimalFactor() {
    // Arrange
    var money = Money.usd(new BigDecimal("100.00"));

    // Act
    var result = money.multiply(new BigDecimal("2.5"));

    // Assert
    assertThat(result.getAmount()).isEqualByComparingTo("250.00");
    assertThat(result.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should multiply Money by integer quantity")
  void shouldMultiplyMoneyByIntegerQuantity() {
    // Arrange
    var money = Money.usd(new BigDecimal("25.00"));

    // Act
    var result = money.multiply(3);

    // Assert
    assertThat(result.getAmount()).isEqualByComparingTo("75.00");
    assertThat(result.getCurrency()).isEqualTo("USD");
  }

  @Test
  @DisplayName("Should throw exception when multiplying by null factor")
  void shouldThrowExceptionWhenMultiplyingByNullFactor() {
    // Arrange
    var money = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThatThrownBy(() -> money.multiply((BigDecimal) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Factor must not be null");
  }

  @Test
  @DisplayName("Should throw exception when multiplying by negative factor")
  void shouldThrowExceptionWhenMultiplyingByNegativeFactor() {
    // Arrange
    var money = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThatThrownBy(() -> money.multiply(new BigDecimal("-2")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Factor must not be negative");
  }

  @Test
  @DisplayName("Should throw exception when multiplying by negative quantity")
  void shouldThrowExceptionWhenMultiplyingByNegativeQuantity() {
    // Arrange
    var money = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThatThrownBy(() -> money.multiply(-2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Quantity must not be negative");
  }

  @Test
  @DisplayName("Should check if Money is zero")
  void shouldCheckIfMoneyIsZero() {
    // Arrange
    var zeroMoney = Money.zero("USD");
    var nonZeroMoney = Money.usd(new BigDecimal("10.00"));

    // Act & Assert
    assertThat(zeroMoney.isZero()).isTrue();
    assertThat(nonZeroMoney.isZero()).isFalse();
  }

  @Test
  @DisplayName("Should check if Money is positive")
  void shouldCheckIfMoneyIsPositive() {
    // Arrange
    var zeroMoney = Money.zero("USD");
    var positiveMoney = Money.usd(new BigDecimal("10.00"));

    // Act & Assert
    assertThat(zeroMoney.isPositive()).isFalse();
    assertThat(positiveMoney.isPositive()).isTrue();
  }

  @Test
  @DisplayName("Should check if Money is greater than another")
  void shouldCheckIfMoneyIsGreaterThanAnother() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.usd(new BigDecimal("50.00"));

    // Act & Assert
    assertThat(money1.isGreaterThan(money2)).isTrue();
    assertThat(money2.isGreaterThan(money1)).isFalse();
  }

  @Test
  @DisplayName("Should check if Money is less than another")
  void shouldCheckIfMoneyIsLessThanAnother() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("50.00"));
    var money2 = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThat(money1.isLessThan(money2)).isTrue();
    assertThat(money2.isLessThan(money1)).isFalse();
  }

  @Test
  @DisplayName("Should throw exception when comparing Money with different currencies")
  void shouldThrowExceptionWhenComparingMoneyWithDifferentCurrencies() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.eur(new BigDecimal("100.00"));

    // Act & Assert
    assertThatThrownBy(() -> money1.isGreaterThan(money2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Currency mismatch");
  }

  @Test
  @DisplayName("Should compare Money values correctly")
  void shouldCompareMoneyValuesCorrectly() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.usd(new BigDecimal("50.00"));
    var money3 = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThat(money1.compareTo(money2)).isPositive();
    assertThat(money2.compareTo(money1)).isNegative();
    assertThat(money1.compareTo(money3)).isZero();
  }

  @Test
  @DisplayName("Should check equality correctly")
  void shouldCheckEqualityCorrectly() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.usd(new BigDecimal("100.00"));
    var money3 = Money.usd(new BigDecimal("50.00"));
    var money4 = Money.eur(new BigDecimal("100.00"));

    // Act & Assert
    assertThat(money1).isEqualTo(money2);
    assertThat(money1).isNotEqualTo(money3);
    assertThat(money1).isNotEqualTo(money4);
    assertThat(money1).isNotEqualTo(null);
    assertThat(money1).isEqualTo(money1);
  }

  @Test
  @DisplayName("Should generate consistent hash codes")
  void shouldGenerateConsistentHashCodes() {
    // Arrange
    var money1 = Money.usd(new BigDecimal("100.00"));
    var money2 = Money.usd(new BigDecimal("100.00"));

    // Act & Assert
    assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
  }

  @Test
  @DisplayName("Should format toString correctly")
  void shouldFormatToStringCorrectly() {
    // Arrange
    var money = Money.usd(new BigDecimal("100.50"));

    // Act
    var result = money.toString();

    // Assert
    assertThat(result).isEqualTo("USD 100.50");
  }
}
