package com.iqscaffold.bookstore.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(BookstoreConstants.FilterOrder.USER_CONTEXT_MDC_FILTER)
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

      MDC.put(BookstoreConstants.MdcKeys.USER_ID, String.valueOf(userContext.userId()));
      MDC.put(BookstoreConstants.MdcKeys.USERNAME, userContext.username());
      MDC.put(BookstoreConstants.MdcKeys.USER_ROLES, String.join(",", userContext.roles()));

      if (userContext.department() != null) {
        MDC.put(BookstoreConstants.MdcKeys.DEPARTMENT, userContext.department());
      }
      if (userContext.organizationId() != null) {
        MDC.put(BookstoreConstants.MdcKeys.ORGANIZATION_ID, userContext.organizationId());
      }
    } else {
      MDC.put(BookstoreConstants.MdcKeys.USER_ID, BookstoreConstants.AnonymousUser.USER_ID);
      MDC.put(BookstoreConstants.MdcKeys.USERNAME, BookstoreConstants.AnonymousUser.USERNAME);
      MDC.put(BookstoreConstants.MdcKeys.USER_ROLES, BookstoreConstants.AnonymousUser.ROLES);
    }
  }

  private void clearUserContextFromMdc() {
    MDC.remove(BookstoreConstants.MdcKeys.USER_ID);
    MDC.remove(BookstoreConstants.MdcKeys.USERNAME);
    MDC.remove(BookstoreConstants.MdcKeys.USER_ROLES);
    MDC.remove(BookstoreConstants.MdcKeys.DEPARTMENT);
    MDC.remove(BookstoreConstants.MdcKeys.ORGANIZATION_ID);
  }
}
