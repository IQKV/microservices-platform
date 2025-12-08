package com.iqscaffold.billingservice.security;

/**
 * JWT claim names used for extracting user context from JWT tokens.
 * 
 * <p>These claim names match the structure of JWT tokens issued by the User Service.
 * The claims are extracted during authentication and used to populate UserContext.
 */
public final class JwtClaimNames {

  /**
   * Subject claim - contains the user ID.
   */
  public static final String SUBJECT = "sub";

  /**
   * Username claim - contains the username.
   */
  public static final String USERNAME = "username";

  /**
   * Email claim - contains the user's email address.
   */
  public static final String EMAIL = "email";

  /**
   * Roles claim - contains the user's authorities (ADMIN, SUPER_ADMIN, USER, etc.).
   * Note: Despite the name "roles", this claim contains authorities.
   */
  public static final String ROLES = "roles";

  /**
   * Tenant ID claim - contains the tenant identifier for multi-tenancy.
   */
  public static final String TENANT_ID = "tenant_id";

  /**
   * First name claim - contains the user's first name.
   */
  public static final String FIRST_NAME = "firstName";

  /**
   * Last name claim - contains the user's last name.
   */
  public static final String LAST_NAME = "lastName";

  private JwtClaimNames() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }
}
