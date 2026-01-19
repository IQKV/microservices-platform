package com.iqscaffold.contactservice.feature;

/**
 * Thread-local holder for feature context.
 * Provides access to feature information throughout the request lifecycle.
 */
public final class FeatureContextHolder {

  private static final ThreadLocal<FeatureContext> contextHolder = new ThreadLocal<>();

  private FeatureContextHolder() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Sets the feature context for the current thread.
   */
  public static void setContext(FeatureContext context) {
    contextHolder.set(context);
  }

  /**
   * Gets the feature context for the current thread.
   * Returns empty context if none is set.
   */
  public static FeatureContext getContext() {
    FeatureContext context = contextHolder.get();
    return context != null ? context : FeatureContext.empty();
  }

  /**
   * Clears the feature context for the current thread.
   */
  public static void clearContext() {
    contextHolder.remove();
  }

  /**
   * Checks if a specific feature is enabled in the current context.
   */
  public static boolean isFeatureEnabled(String featureName) {
    return getContext().isFeatureEnabled(featureName);
  }

  /**
   * Gets the quota for a specific feature in the current context.
   */
  public static Integer getQuota(String featureName) {
    return getContext().getQuota(featureName);
  }

  /**
   * Gets the limit for a specific feature in the current context.
   */
  public static Integer getLimit(String featureName) {
    return getContext().getLimit(featureName);
  }

  /**
   * Gets the tier for a specific feature in the current context.
   */
  public static String getTier(String featureName) {
    return getContext().getTier(featureName);
  }
}