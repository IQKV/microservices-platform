package com.iqscaffold.bookstore.shared;

import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Objects;

/**
 * Base class for Aggregate Roots in DDD.
 * Provides identity management and equality based on ID.
 *
 * @param <IdT> the type of the identifier
 */
@MappedSuperclass
public abstract class AggregateRoot<IdT extends Serializable> {

  /**
   * Returns the identifier of this aggregate root.
   */
  public abstract IdT getId();

  /**
   * Checks if this aggregate has an assigned ID.
   */
  public boolean hasId() {
    return getId() != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AggregateRoot<?> that = (AggregateRoot<?>) o;
    return hasId() && that.hasId() && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return hasId() ? Objects.hash(getId()) : super.hashCode();
  }
}
