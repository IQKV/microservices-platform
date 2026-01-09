package com.iqscaffold.billingservice.security;

/**
 * Standard JWT claim names used across the IQ Scaffold platform.
 * Follows JWT RFC conventions using snake_case for claim names.
 */
public final class JwtClaimNames {
  private JwtClaimNames() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  // Standard JWT claims (RFC 7519)
  public static final String SUBJECT = "sub";
  public static final String ISSUER = "iss";
  public static final String ISSUED_AT = "iat";
  public static final String EXPIRATION = "exp";
  public static final String JWT_ID = "jti";

  // Custom IQ Scaffold claims
  public static final String TYPE = "type";
  public static final String USERNAME = "username";
  public static final String EMAIL = "email";
  public static final String AUTHORITIES = "authorities";
  public static final String PERMISSIONS = "permissions";
  public static final String FIRST_NAME = "firstName";
  public static final String LAST_NAME = "lastName";
  public static final String TENANT_ID = "tenant_id";
  public static final String ORGANIZATION_ID = "organizationId";
  public static final String PREFERRED_LOCALE = "preferred_locale";

  // Token types
  public static final String TOKEN_TYPE_ACCESS = "access";
  public static final String TOKEN_TYPE_REFRESH = "refresh";
}
