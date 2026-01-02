package com.iqscaffold.billingservice.security;

public final class JwtClaimNames {
  private JwtClaimNames() {}

  public static final String SUBJECT = "sub";
  public static final String USERNAME = "username";
  public static final String EMAIL = "email";
  public static final String ROLES = "roles";
  public static final String TENANT_ID = "tenant_id";
  public static final String FIRST_NAME = "first_name";
  public static final String LAST_NAME = "last_name";
}
