package org.gripday.userservice.tenancy;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SchemaNameResolver {

  private final String prefix;
  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
  private static final Pattern ILLEGAL = Pattern.compile("[^a-z0-9_]");

  public SchemaNameResolver(@Value("${app.tenancy.schema.prefix:tenant_}") final String prefix) {
    this.prefix = prefix;
  }

  public String toSchema(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "public";
    }
    return cache.computeIfAbsent(tenantId, this::normalize);
  }

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