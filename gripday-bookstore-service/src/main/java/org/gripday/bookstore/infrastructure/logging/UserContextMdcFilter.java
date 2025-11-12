package org.gripday.bookstore.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.gripday.bookstore.infrastructure.security.UserContextExtractor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class UserContextMdcFilter extends OncePerRequestFilter {

  private final UserContextExtractor userContextExtractor;

  public UserContextMdcFilter(final UserContextExtractor userContextExtractor) {
    this.userContextExtractor = userContextExtractor;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      addUserContextToMdc();
      filterChain.doFilter(request, response);
    } finally {
      clearUserContextFromMdc();
    }
  }

  private void addUserContextToMdc() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtToken) {
      var jwt = jwtToken.getToken();
      var userContext = userContextExtractor.extractFromJwt(jwt);

      MDC.put("userId", String.valueOf(userContext.userId()));
      MDC.put("username", userContext.username());
      MDC.put("userRoles", String.join(",", userContext.roles()));

      if (userContext.department() != null) {
        MDC.put("department", userContext.department());
      }
      if (userContext.organizationId() != null) {
        MDC.put("organizationId", userContext.organizationId());
      }
    } else {
      MDC.put("userId", "anonymous");
      MDC.put("username", "anonymous");
      MDC.put("userRoles", "NONE");
    }
  }

  private void clearUserContextFromMdc() {
    MDC.remove("userId");
    MDC.remove("username");
    MDC.remove("userRoles");
    MDC.remove("department");
    MDC.remove("organizationId");
  }
}