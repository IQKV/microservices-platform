package com.iqscaffold.userservice.tenancy;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Utility class for generating unique tenant IDs from organization names.
 * Tenant IDs are normalized, slugified, and suffixed with random characters for uniqueness.
 */
@Component
public class TenantIdGenerator {

  private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
  private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");
  private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final int SUFFIX_LENGTH = 4;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final TenantRepository tenantRepository;

  public TenantIdGenerator(final TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  /**
   * Generate a unique tenant ID from an organization name.
   * Creates a slug from the name and appends a random suffix for uniqueness.
   * Ensures the generated ID doesn't already exist in the database.
   *
   * @param organizationName the organization name to generate from
   * @return a unique, normalized tenant ID
   */
  public String generateFromOrganizationName(String organizationName) {
    var baseSlug = createSlug(organizationName);
    
    // Limit base slug length to allow for suffix
    if (baseSlug.length() > 46) {
      baseSlug = baseSlug.substring(0, 46);
    }
    
    // Try to generate a unique ID (max 10 attempts)
    for (var i = 0; i < 10; i++) {
      var suffix = generateRandomSuffix();
      var tenantId = baseSlug + "-" + suffix;
      
      if (!tenantRepository.existsByTenantId(tenantId)) {
        return tenantId;
      }
    }
    
    // Fallback: use timestamp-based suffix if random attempts fail
    var timestamp = System.currentTimeMillis() % 100000;
    return baseSlug + "-" + timestamp;
  }

  /**
   * Create a URL-friendly slug from text.
   * Converts to lowercase, removes special characters, and replaces spaces with dashes.
   *
   * @param text the text to slugify
   * @return a normalized slug
   */
  private String createSlug(String text) {
    // Normalize to NFD form and remove diacritics
    var normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
    
    // Convert to lowercase
    var lowercase = normalized.toLowerCase(Locale.ROOT);
    
    // Replace whitespace with single dash
    var noWhitespace = WHITESPACE.matcher(lowercase).replaceAll("-");
    
    // Remove all non-latin characters except dashes
    var latin = NON_LATIN.matcher(noWhitespace).replaceAll("");
    
    // Replace multiple consecutive dashes with single dash
    var slug = MULTIPLE_DASHES.matcher(latin).replaceAll("-");
    
    // Remove leading and trailing dashes
    slug = slug.replaceAll("^-+|-+$", "");
    
    // Ensure minimum length
    if (slug.isEmpty() || slug.length() < 3) {
      slug = "tenant";
    }
    
    return slug;
  }

  /**
   * Generate a random alphanumeric suffix for uniqueness.
   *
   * @return a random suffix of SUFFIX_LENGTH characters
   */
  private String generateRandomSuffix() {
    var suffix = new StringBuilder(SUFFIX_LENGTH);
    for (var i = 0; i < SUFFIX_LENGTH; i++) {
      var randomIndex = RANDOM.nextInt(ALLOWED_CHARS.length());
      suffix.append(ALLOWED_CHARS.charAt(randomIndex));
    }
    return suffix.toString();
  }

  /**
   * Validate if a tenant ID meets the format requirements.
   * Must be 3-50 characters, contain only alphanumeric characters and dashes,
   * and not start or end with a dash.
   *
   * @param tenantId the tenant ID to validate
   * @return true if valid, false otherwise
   */
  public boolean isValid(String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      return false;
    }
    
    var length = tenantId.length();
    if (length < 3 || length > 50) {
      return false;
    }
    
    // Must contain only lowercase letters, numbers, and dashes
    if (!tenantId.matches("^[a-z0-9-]+$")) {
      return false;
    }
    
    // Cannot start or end with dash
    if (tenantId.startsWith("-") || tenantId.endsWith("-")) {
      return false;
    }
    
    // Cannot contain consecutive dashes
    if (tenantId.contains("--")) {
      return false;
    }
    
    return true;
  }

  /**
   * Check if a tenant ID is available (not already in use).
   *
   * @param tenantId the tenant ID to check
   * @return true if available, false if already exists
   */
  public boolean isAvailable(String tenantId) {
    return !tenantRepository.existsByTenantId(tenantId);
  }
}
