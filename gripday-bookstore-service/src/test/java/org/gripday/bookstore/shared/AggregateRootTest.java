package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AggregateRoot Tests")
class AggregateRootTest {

  // Test implementation of AggregateRoot
  static class TestAggregate extends AggregateRoot<Long> {
    private Long id;

    public TestAggregate(final Long id) {
      this.id = id;
    }

    @Override
    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }
  }

  @Test
  @DisplayName("Should check if aggregate has ID")
  void shouldCheckIfAggregateHasId() {
    // Arrange
    var aggregateWithId = new TestAggregate(1L);
    var aggregateWithoutId = new TestAggregate(null);

    // Act & Assert
    assertThat(aggregateWithId.hasId()).isTrue();
    assertThat(aggregateWithoutId.hasId()).isFalse();
  }

  @Test
  @DisplayName("Should be equal when same instance")
  void shouldBeEqualWhenSameInstance() {
    // Arrange
    var aggregate = new TestAggregate(1L);

    // Act & Assert
    assertThat(aggregate).isEqualTo(aggregate);
  }

  @Test
  @DisplayName("Should be equal when same ID")
  void shouldBeEqualWhenSameId() {
    // Arrange
    var aggregate1 = new TestAggregate(1L);
    var aggregate2 = new TestAggregate(1L);

    // Act & Assert
    assertThat(aggregate1).isEqualTo(aggregate2);
  }

  @Test
  @DisplayName("Should not be equal when different IDs")
  void shouldNotBeEqualWhenDifferentIds() {
    // Arrange
    var aggregate1 = new TestAggregate(1L);
    var aggregate2 = new TestAggregate(2L);

    // Act & Assert
    assertThat(aggregate1).isNotEqualTo(aggregate2);
  }

  @Test
  @DisplayName("Should not be equal when one has no ID")
  void shouldNotBeEqualWhenOneHasNoId() {
    // Arrange
    var aggregateWithId = new TestAggregate(1L);
    var aggregateWithoutId = new TestAggregate(null);

    // Act & Assert
    assertThat(aggregateWithId).isNotEqualTo(aggregateWithoutId);
  }

  @Test
  @DisplayName("Should not be equal when both have no ID")
  void shouldNotBeEqualWhenBothHaveNoId() {
    // Arrange
    var aggregate1 = new TestAggregate(null);
    var aggregate2 = new TestAggregate(null);

    // Act & Assert
    assertThat(aggregate1).isNotEqualTo(aggregate2);
  }

  @Test
  @DisplayName("Should not be equal to null")
  void shouldNotBeEqualToNull() {
    // Arrange
    var aggregate = new TestAggregate(1L);

    // Act & Assert
    assertThat(aggregate).isNotEqualTo(null);
  }

  @Test
  @DisplayName("Should not be equal to different class")
  void shouldNotBeEqualToDifferentClass() {
    // Arrange
    var aggregate = new TestAggregate(1L);
    var differentObject = "not an aggregate";

    // Act & Assert
    assertThat(aggregate).isNotEqualTo(differentObject);
  }

  @Test
  @DisplayName("Should have same hash code when same ID")
  void shouldHaveSameHashCodeWhenSameId() {
    // Arrange
    var aggregate1 = new TestAggregate(1L);
    var aggregate2 = new TestAggregate(1L);

    // Act & Assert
    assertThat(aggregate1.hashCode()).isEqualTo(aggregate2.hashCode());
  }

  @Test
  @DisplayName("Should have different hash code when no ID")
  void shouldHaveDifferentHashCodeWhenNoId() {
    // Arrange
    var aggregate1 = new TestAggregate(null);
    var aggregate2 = new TestAggregate(null);

    // Act & Assert
    // Without ID, hash codes are based on Object.hashCode() which will differ
    assertThat(aggregate1.hashCode()).isNotEqualTo(aggregate2.hashCode());
  }

  @Test
  @DisplayName("Should maintain consistent hash code")
  void shouldMaintainConsistentHashCode() {
    // Arrange
    var aggregate = new TestAggregate(1L);
    var firstHashCode = aggregate.hashCode();

    // Act
    var secondHashCode = aggregate.hashCode();

    // Assert
    assertThat(firstHashCode).isEqualTo(secondHashCode);
  }
}
