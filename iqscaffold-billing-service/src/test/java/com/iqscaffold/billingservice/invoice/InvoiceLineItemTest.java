package com.iqscaffold.billingservice.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for InvoiceLineItem value object.
 *
 * <p>Tests line item calculations including:
 * <ul>
 *   <li>Amount calculations (quantity * unitPrice)</li>
 *   <li>Factory methods for different line item types</li>
 *   <li>Validation of invariants</li>
 *   <li>Precision and rounding for financial calculations</li>
 * </ul>
 *
 * <p>Ensures 100% coverage for financial calculations.
 */
@DisplayName("InvoiceLineItem Value Object Tests")
class InvoiceLineItemTest {

  @Nested
  @DisplayName("Line Item Creation Tests")
  class LineItemCreationTests {

    @Test
    @DisplayName("Should create line item with valid parameters")
    void shouldCreateLineItemWithValidParameters() {
      // Act
      InvoiceLineItem lineItem = new InvoiceLineItem(
          InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE,
          "Pro Plan - Monthly",
          1L,
          new BigDecimal("49.99"),
          new BigDecimal("49.99")
      );

      // Assert
      assertThat(lineItem).isNotNull();
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE);
      assertThat(lineItem.description()).isEqualTo("Pro Plan - Monthly");
      assertThat(lineItem.quantity()).isEqualTo(1L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    @Test
    @DisplayName("Should calculate amount when not provided")
    void shouldCalculateAmountWhenNotProvided() {
      // Act
      InvoiceLineItem lineItem = new InvoiceLineItem(
          InvoiceLineItem.LineItemType.USAGE_CHARGE,
          "API calls",
          1000L,
          new BigDecimal("0.01"),
          null
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should throw exception when type is null")
    void shouldThrowExceptionWhenTypeIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new InvoiceLineItem(
          null,
          "Description",
          1L,
          new BigDecimal("10.00"),
          new BigDecimal("10.00")
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Line item type cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when description is blank")
    void shouldThrowExceptionWhenDescriptionIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> new InvoiceLineItem(
          InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE,
          "   ",
          1L,
          new BigDecimal("10.00"),
          new BigDecimal("10.00")
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Line item description cannot be blank");
    }

    @Test
    @DisplayName("Should throw exception when quantity is zero")
    void shouldThrowExceptionWhenQuantityIsZero() {
      // Act & Assert
      assertThatThrownBy(() -> new InvoiceLineItem(
          InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE,
          "Description",
          0L,
          new BigDecimal("10.00"),
          new BigDecimal("10.00")
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Line item quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception when quantity is negative")
    void shouldThrowExceptionWhenQuantityIsNegative() {
      // Act & Assert
      assertThatThrownBy(() -> new InvoiceLineItem(
          InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE,
          "Description",
          -1L,
          new BigDecimal("10.00"),
          new BigDecimal("10.00")
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Line item quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception when unit price is negative")
    void shouldThrowExceptionWhenUnitPriceIsNegative() {
      // Act & Assert
      assertThatThrownBy(() -> new InvoiceLineItem(
          InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE,
          "Description",
          1L,
          new BigDecimal("-10.00"),
          new BigDecimal("-10.00")
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Line item unit price must be non-negative");
    }

    @Test
    @DisplayName("Should throw exception when amount doesn't match calculation")
    void shouldThrowExceptionWhenAmountDoesntMatchCalculation() {
      // Act & Assert
      assertThatThrownBy(() -> new InvoiceLineItem(
          InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE,
          "Description",
          2L,
          new BigDecimal("10.00"),
          new BigDecimal("15.00") // Should be 20.00
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Line item amount must equal quantity * unitPrice");
    }
  }

  @Nested
  @DisplayName("Subscription Fee Factory Method Tests")
  class SubscriptionFeeFactoryMethodTests {

    @Test
    @DisplayName("Should create subscription fee line item")
    void shouldCreateSubscriptionFeeLineItem() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.subscriptionFee(
          "Pro Plan - Monthly",
          new BigDecimal("49.99")
      );

      // Assert
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.SUBSCRIPTION_FEE);
      assertThat(lineItem.description()).isEqualTo("Pro Plan - Monthly");
      assertThat(lineItem.quantity()).isEqualTo(1L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    @Test
    @DisplayName("Should maintain 2 decimal places for subscription fee")
    void shouldMaintainTwoDecimalPlacesForSubscriptionFee() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.subscriptionFee(
          "Plan",
          new BigDecimal("49.999")
      );

      // Assert
      assertThat(lineItem.amount().scale()).isEqualTo(2);
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
  }

  @Nested
  @DisplayName("Usage Charge Factory Method Tests")
  class UsageChargeFactoryMethodTests {

    @Test
    @DisplayName("Should create usage charge line item")
    void shouldCreateUsageChargeLineItem() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "API calls overage",
          1000L,
          new BigDecimal("0.01")
      );

      // Assert
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.USAGE_CHARGE);
      assertThat(lineItem.description()).isEqualTo("API calls overage");
      assertThat(lineItem.quantity()).isEqualTo(1000L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("0.01"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should calculate correct amount for large quantities")
    void shouldCalculateCorrectAmountForLargeQuantities() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Storage GB-hours",
          50000L,
          new BigDecimal("0.001")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should handle fractional unit prices correctly")
    void shouldHandleFractionalUnitPricesCorrectly() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Compute hours",
          100L,
          new BigDecimal("0.0123")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("1.23"));
    }
  }

  @Nested
  @DisplayName("Proration Credit Factory Method Tests")
  class ProrationCreditFactoryMethodTests {

    @Test
    @DisplayName("Should create proration credit line item")
    void shouldCreateProrationCreditLineItem() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.prorationCredit(
          "Credit for unused time",
          new BigDecimal("25.00")
      );

      // Assert
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.PRORATION_CREDIT);
      assertThat(lineItem.description()).isEqualTo("Credit for unused time");
      assertThat(lineItem.quantity()).isEqualTo(1L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("-25.00"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("-25.00"));
      assertThat(lineItem.isCredit()).isTrue();
      assertThat(lineItem.isCharge()).isFalse();
    }

    @Test
    @DisplayName("Should negate positive amount for credit")
    void shouldNegatePositiveAmountForCredit() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.prorationCredit(
          "Credit",
          new BigDecimal("50.00")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("-50.00"));
    }
  }

  @Nested
  @DisplayName("Proration Charge Factory Method Tests")
  class ProrationChargeFactoryMethodTests {

    @Test
    @DisplayName("Should create proration charge line item")
    void shouldCreateProrationChargeLineItem() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.prorationCharge(
          "Charge for new plan",
          new BigDecimal("50.00")
      );

      // Assert
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.PRORATION_CHARGE);
      assertThat(lineItem.description()).isEqualTo("Charge for new plan");
      assertThat(lineItem.quantity()).isEqualTo(1L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
      assertThat(lineItem.isCharge()).isTrue();
      assertThat(lineItem.isCredit()).isFalse();
    }
  }

  @Nested
  @DisplayName("Discount Factory Method Tests")
  class DiscountFactoryMethodTests {

    @Test
    @DisplayName("Should create discount line item")
    void shouldCreateDiscountLineItem() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.discount(
          "Promotional discount",
          new BigDecimal("10.00")
      );

      // Assert
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.DISCOUNT);
      assertThat(lineItem.description()).isEqualTo("Promotional discount");
      assertThat(lineItem.quantity()).isEqualTo(1L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("-10.00"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("-10.00"));
      assertThat(lineItem.isCredit()).isTrue();
    }

    @Test
    @DisplayName("Should negate positive amount for discount")
    void shouldNegatePositiveAmountForDiscount() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.discount(
          "Discount",
          new BigDecimal("15.50")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("-15.50"));
    }
  }

  @Nested
  @DisplayName("Tax Factory Method Tests")
  class TaxFactoryMethodTests {

    @Test
    @DisplayName("Should create tax line item")
    void shouldCreateTaxLineItem() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.tax(
          "Sales tax (10%)",
          new BigDecimal("5.00")
      );

      // Assert
      assertThat(lineItem.type()).isEqualTo(InvoiceLineItem.LineItemType.TAX);
      assertThat(lineItem.description()).isEqualTo("Sales tax (10%)");
      assertThat(lineItem.quantity()).isEqualTo(1L);
      assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("5.00"));
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("5.00"));
      assertThat(lineItem.isCharge()).isTrue();
    }
  }

  @Nested
  @DisplayName("Line Item Query Methods Tests")
  class LineItemQueryMethodsTests {

    @Test
    @DisplayName("Should identify credit line items")
    void shouldIdentifyCreditLineItems() {
      // Arrange
      InvoiceLineItem credit = InvoiceLineItem.prorationCredit(
          "Credit",
          new BigDecimal("25.00")
      );
      InvoiceLineItem charge = InvoiceLineItem.subscriptionFee(
          "Fee",
          new BigDecimal("49.99")
      );

      // Assert
      assertThat(credit.isCredit()).isTrue();
      assertThat(credit.isCharge()).isFalse();
      assertThat(charge.isCredit()).isFalse();
      assertThat(charge.isCharge()).isTrue();
    }

    @Test
    @DisplayName("Should get absolute amount")
    void shouldGetAbsoluteAmount() {
      // Arrange
      InvoiceLineItem credit = InvoiceLineItem.discount(
          "Discount",
          new BigDecimal("15.00")
      );

      // Act
      BigDecimal absoluteAmount = credit.getAbsoluteAmount();

      // Assert
      assertThat(absoluteAmount).isEqualByComparingTo(new BigDecimal("15.00"));
    }
  }

  @Nested
  @DisplayName("Financial Calculation Precision Tests")
  class FinancialCalculationPrecisionTests {

    @Test
    @DisplayName("Should maintain 2 decimal places for all amounts")
    void shouldMaintainTwoDecimalPlacesForAllAmounts() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Usage",
          333L,
          new BigDecimal("0.003")
      );

      // Assert
      assertThat(lineItem.unitPrice().scale()).isEqualTo(2);
      assertThat(lineItem.amount().scale()).isEqualTo(2);
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Should round half up for monetary values")
    void shouldRoundHalfUpForMonetaryValues() {
      // Act
      InvoiceLineItem lineItem1 = InvoiceLineItem.usageCharge(
          "Usage 1",
          1L,
          new BigDecimal("10.555")
      );
      InvoiceLineItem lineItem2 = InvoiceLineItem.usageCharge(
          "Usage 2",
          1L,
          new BigDecimal("10.554")
      );

      // Assert
      assertThat(lineItem1.amount()).isEqualByComparingTo(new BigDecimal("10.56"));
      assertThat(lineItem2.amount()).isEqualByComparingTo(new BigDecimal("10.55"));
    }

    @Test
    @DisplayName("Should handle complex quantity and unit price calculations")
    void shouldHandleComplexQuantityAndUnitPriceCalculations() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Complex calculation",
          12345L,
          new BigDecimal("0.00789")
      );

      // Assert
      // 12345 * 0.00789 = 97.40205 ≈ 97.40
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("97.40"));
      assertThat(lineItem.amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle very small unit prices")
    void shouldHandleVerySmallUnitPrices() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Micro-transactions",
          1000000L,
          new BigDecimal("0.00001")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should handle zero unit price")
    void shouldHandleZeroUnitPrice() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Free tier usage",
          1000L,
          BigDecimal.ZERO
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("Value Object Equality Tests")
  class ValueObjectEqualityTests {

    @Test
    @DisplayName("Should be equal when all fields match")
    void shouldBeEqualWhenAllFieldsMatch() {
      // Arrange
      InvoiceLineItem lineItem1 = InvoiceLineItem.subscriptionFee(
          "Pro Plan",
          new BigDecimal("49.99")
      );
      InvoiceLineItem lineItem2 = InvoiceLineItem.subscriptionFee(
          "Pro Plan",
          new BigDecimal("49.99")
      );

      // Assert
      assertThat(lineItem1).isEqualTo(lineItem2);
      assertThat(lineItem1.hashCode()).isEqualTo(lineItem2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when amounts differ")
    void shouldNotBeEqualWhenAmountsDiffer() {
      // Arrange
      InvoiceLineItem lineItem1 = InvoiceLineItem.subscriptionFee(
          "Pro Plan",
          new BigDecimal("49.99")
      );
      InvoiceLineItem lineItem2 = InvoiceLineItem.subscriptionFee(
          "Pro Plan",
          new BigDecimal("59.99")
      );

      // Assert
      assertThat(lineItem1).isNotEqualTo(lineItem2);
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
      // Arrange
      InvoiceLineItem lineItem = InvoiceLineItem.subscriptionFee(
          "Pro Plan",
          new BigDecimal("49.99")
      );

      // Act
      String toString = lineItem.toString();

      // Assert
      assertThat(toString).contains("Pro Plan");
      assertThat(toString).contains("49.99");
      assertThat(toString).contains("SUBSCRIPTION_FEE");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle maximum reasonable quantity")
    void shouldHandleMaximumReasonableQuantity() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Large volume",
          999999999L,
          new BigDecimal("0.000001")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should handle maximum reasonable unit price")
    void shouldHandleMaximumReasonableUnitPrice() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.subscriptionFee(
          "Enterprise plan",
          new BigDecimal("999999.99")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("999999.99"));
    }

    @Test
    @DisplayName("Should handle minimum positive unit price")
    void shouldHandleMinimumPositiveUnitPrice() {
      // Act
      InvoiceLineItem lineItem = InvoiceLineItem.usageCharge(
          "Micro-pricing",
          1L,
          new BigDecimal("0.01")
      );

      // Assert
      assertThat(lineItem.amount()).isEqualByComparingTo(new BigDecimal("0.01"));
    }
  }
}
