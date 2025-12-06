package com.iqscaffold.billingservice.shared.specification;

/**
 * Specification pattern interface for encapsulating business rules.
 * 
 * <p>Specifications represent business rules that can be evaluated against domain objects.
 * They make business logic explicit, testable, and reusable. Specifications can be combined
 * using AND, OR, and NOT operators to create complex validation logic.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Specification<Subscription> activeSpec = new ActiveSubscriptionSpecification();
 * Specification<Subscription> trialSpec = new TrialSubscriptionSpecification();
 * 
 * // Combine specifications
 * Specification<Subscription> activeOrTrial = activeSpec.or(trialSpec);
 * Specification<Subscription> notActive = activeSpec.not();
 * 
 * // Evaluate
 * if (activeOrTrial.isSatisfiedBy(subscription)) {
 *   // Business logic
 * }
 * }</pre>
 * 
 * @param <T> the type of object this specification evaluates
 */
public interface Specification<T> {

  /**
   * Checks if the given object satisfies this specification.
   * 
   * @param candidate the object to evaluate
   * @return true if the object satisfies the specification, false otherwise
   */
  boolean isSatisfiedBy(T candidate);

  /**
   * Creates a new specification that is the logical AND of this specification and another.
   * The resulting specification is satisfied only if both specifications are satisfied.
   * 
   * @param other the other specification to combine with
   * @return a new specification representing the AND combination
   */
  default Specification<T> and(final Specification<T> other) {
    return new AndSpecification<>(this, other);
  }

  /**
   * Creates a new specification that is the logical OR of this specification and another.
   * The resulting specification is satisfied if either specification is satisfied.
   * 
   * @param other the other specification to combine with
   * @return a new specification representing the OR combination
   */
  default Specification<T> or(final Specification<T> other) {
    return new OrSpecification<>(this, other);
  }

  /**
   * Creates a new specification that is the logical NOT of this specification.
   * The resulting specification is satisfied when this specification is not satisfied.
   * 
   * @return a new specification representing the NOT of this specification
   */
  default Specification<T> not() {
    return new NotSpecification<>(this);
  }

  /**
   * Composite specification representing the logical AND of two specifications.
   * 
   * @param <T> the type of object this specification evaluates
   */
  class AndSpecification<T> implements Specification<T> {
    private final Specification<T> left;
    private final Specification<T> right;

    public AndSpecification(final Specification<T> left, final Specification<T> right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean isSatisfiedBy(final T candidate) {
      return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate);
    }
  }

  /**
   * Composite specification representing the logical OR of two specifications.
   * 
   * @param <T> the type of object this specification evaluates
   */
  class OrSpecification<T> implements Specification<T> {
    private final Specification<T> left;
    private final Specification<T> right;

    public OrSpecification(final Specification<T> left, final Specification<T> right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean isSatisfiedBy(final T candidate) {
      return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate);
    }
  }

  /**
   * Composite specification representing the logical NOT of a specification.
   * 
   * @param <T> the type of object this specification evaluates
   */
  class NotSpecification<T> implements Specification<T> {
    private final Specification<T> spec;

    public NotSpecification(final Specification<T> spec) {
      this.spec = spec;
    }

    @Override
    public boolean isSatisfiedBy(final T candidate) {
      return !spec.isSatisfiedBy(candidate);
    }
  }
}
