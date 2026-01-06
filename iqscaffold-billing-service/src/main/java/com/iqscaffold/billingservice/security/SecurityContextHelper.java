package com.iqscaffold.billingservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class SecurityContextHelper {

  private SecurityContextHelper() {}

  public static UserContext getCurrentUserContext() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      Object userContext = attributes.getRequest().getAttribute("userContext");
      if (userContext instanceof UserContext uc) {
        return uc;
      }
    }
    return null;
  }

  public static UserContext getCurrentUserContextOrThrow() {
    UserContext context = getCurrentUserContext();
    if (context == null) {
      throw new IllegalStateException("User context not found in request");
    }
    return context;
  }

  public static Long getCurrentUserId() {
    UserContext uc = getCurrentUserContext();
    return uc != null ? uc.userId() : null;
  }
  
  public static String getCurrentTenantId() {
      UserContext uc = getCurrentUserContext();
      return uc != null ? uc.tenantId() : null;
  }
}
