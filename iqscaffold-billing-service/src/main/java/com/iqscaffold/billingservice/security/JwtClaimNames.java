package com.iqscaffold.billingservice.security;

public final class JwtClaimNames {
  private JwtClaimNames() {
  }

  public static final String SUBJECT = "sub";
  public static final String USERNAME = "username";
  public static final String EMAIL = "email";
  public static final String AUTHORITIES = "authorities";
  public static final String TENANT_ID = "tenantId";
  public static final String FIRST_NAME = "firstName";
  public static final String LAST_NAME = "lastName";
}
