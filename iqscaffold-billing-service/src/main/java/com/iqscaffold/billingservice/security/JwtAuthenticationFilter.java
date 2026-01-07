package com.iqscaffold.billingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = request.getHeader("X-Correlation-ID");
    if (correlationId != null) {
      org.slf4j.MDC.put("correlationId", correlationId);
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
      Jwt jwt = jwtAuthToken.getToken();
      UserContext userContext = extractUserContext(jwt);

      if (userContext.tenantId() != null) {
        TenantContext.setCurrentTenantId(userContext.tenantId());
      }
      request.setAttribute("userContext", userContext);
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      org.slf4j.MDC.clear();
    }
  }

  private UserContext extractUserContext(Jwt jwt) {
    Long userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));
    String username = jwt.getClaim(JwtClaimNames.USERNAME);
    String email = jwt.getClaim(JwtClaimNames.EMAIL);
    Set<String> authorities = extractAuthorities(jwt.getClaim(JwtClaimNames.AUTHORITIES));
    String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);
    String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
    String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);

    return new UserContext(userId, username, email, authorities, tenantId, firstName, lastName);
  }

  private Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (NumberFormatException e) {
          yield null;
        }
      }
      case null, default -> null;
    };
  }

  @SuppressWarnings("unchecked")
  private Set<String> extractAuthorities(Object value) {
    return switch (value) {
      case List<?> list -> new HashSet<>(list.stream().map(Object::toString).toList());
      case Set<?> set -> new HashSet<>(set.stream().map(Object::toString).toList());
      case null, default -> Collections.emptySet();
    };
  }
}
