package com.iqscaffold.billingservice.tenancy;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves tenant IDs to database schema names.
 * Normalizes tenant IDs to valid PostgreSQL schema names and caches results for performance.
 */
@Component
public class SchemaNameResolver {

  private final String prefix;
  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
  private static final Pattern ILLEGAL = Pattern.compile("[^a-z0-9_]");

  public SchemaNameResolver(@Value("${iqscaffold.billing.tenancy.schema.prefix:tenant_}") final String prefix) {
    this.prefix = prefix;
  }

  /**
   * Convert a tenant ID to a schema name.
   * Returns "public" for null or blank tenant IDs.
   *
   * @param tenantId the tenant ID
   * @return the schema name
   */
  public String toSchema(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "public";
    }
    return cache.computeIfAbsent(tenantId, this::normalize);
  }

  /**
   * Normalize a tenant ID to a valid PostgreSQL schema name.
   * - Converts to lowercase
   * - Replaces hyphens with underscores
   * - Removes illegal characters
   * - Truncates to 48 characters (leaving room for prefix)
   * - Adds configured prefix
   *
   * @param tenantId the tenant ID to normalize
   * @return the normalized schema name
   */
  private String normalize(String tenantId) {
    var lower = tenantId.toLowerCase(Locale.ROOT).trim();
    var replaced = lower.replace('-', '_');
    var cleaned = ILLEGAL.matcher(replaced).replaceAll("");
    if (cleaned.length() > 48) {
      cleaned = cleaned.substring(0, 48);
    }
    return prefix + cleaned;
  }
}
