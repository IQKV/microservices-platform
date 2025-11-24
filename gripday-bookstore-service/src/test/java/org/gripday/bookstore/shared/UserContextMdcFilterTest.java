package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserContextMdcFilter Tests")
class UserContextMdcFilterTest {

  @Mock
  private UserContextExtractor userContextExtractor;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private UserContextMdcFilter filter;

  @BeforeEach
  void setUp() {
    filter = new UserContextMdcFilter(userContextExtractor);
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should add user context to MDC when JWT authentication is present")
  void shouldAddUserContextToMdcWhenJwtAuthenticationIsPresent() throws Exception {
    // Arrange
    var jwt = createJwt();
    var jwtToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(jwtToken);

    var userContext = new UserContext(
        1L,
        "johndoe",
        "john@example.com",
        Set.of("USER", "ADMIN"),
        Set.of("read:books"),
        "Engineering",
        "org-123",
        Map.of()
    );

    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Capture MDC values during filter execution
    doAnswer(invocation -> {
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isEqualTo("1");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isEqualTo("johndoe");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).contains("USER", "ADMIN");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.DEPARTMENT)).isEqualTo("Engineering");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.ORGANIZATION_ID)).isEqualTo("org-123");
      return null;
    }).when(filterChain).doFilter(request, response);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should add user context to MDC without optional fields")
  void shouldAddUserContextToMdcWithoutOptionalFields() throws Exception {
    // Arrange
    var jwt = createJwt();
    var jwtToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(jwtToken);

    var userContext = new UserContext(
        2L,
        "janedoe",
        "jane@example.com",
        Set.of("USER"),
        Set.of(),
        null,
        null,
        Map.of()
    );

    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Capture MDC values during filter execution
    doAnswer(invocation -> {
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isEqualTo("2");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isEqualTo("janedoe");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).isEqualTo("USER");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.DEPARTMENT)).isNull();
      assertThat(MDC.get(BookstoreConstants.MdcKeys.ORGANIZATION_ID)).isNull();
      return null;
    }).when(filterChain).doFilter(request, response);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should add anonymous user to MDC when no authentication is present")
  void shouldAddAnonymousUserToMdcWhenNoAuthenticationIsPresent() throws Exception {
    // Arrange
    SecurityContextHolder.getContext().setAuthentication(null);

    // Capture MDC values during filter execution
    doAnswer(invocation -> {
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isEqualTo("anonymous");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isEqualTo("anonymous");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).isEqualTo("NONE");
      return null;
    }).when(filterChain).doFilter(request, response);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should add anonymous user to MDC when authentication is not JWT")
  void shouldAddAnonymousUserToMdcWhenAuthenticationIsNotJwt() throws Exception {
    // Arrange
    var authentication = new UsernamePasswordAuthenticationToken("user", "password");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Capture MDC values during filter execution
    doAnswer(invocation -> {
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isEqualTo("anonymous");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isEqualTo("anonymous");
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).isEqualTo("NONE");
      return null;
    }).when(filterChain).doFilter(request, response);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should clear MDC after filter chain execution")
  void shouldClearMdcAfterFilterChainExecution() throws Exception {
    // Arrange
    var jwt = createJwt();
    var jwtToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(jwtToken);

    var userContext = new UserContext(
        1L,
        "johndoe",
        "john@example.com",
        Set.of("USER"),
        Set.of(),
        "Engineering",
        "org-123",
        Map.of()
    );

    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert - MDC should be cleared after filter execution
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.DEPARTMENT)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.ORGANIZATION_ID)).isNull();
  }

  @Test
  @DisplayName("Should clear MDC even when filter chain throws exception")
  void shouldClearMdcEvenWhenFilterChainThrowsException() throws Exception {
    // Arrange
    var jwt = createJwt();
    var jwtToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(jwtToken);

    var userContext = new UserContext(
        1L,
        "johndoe",
        "john@example.com",
        Set.of("USER"),
        Set.of(),
        "Engineering",
        "org-123",
        Map.of()
    );

    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);
    doThrow(new RuntimeException("Filter chain error")).when(filterChain).doFilter(any(), any());

    // Act & Assert
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (RuntimeException e) {
      // Expected exception
    }

    // Assert - MDC should still be cleared even after exception
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.DEPARTMENT)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.ORGANIZATION_ID)).isNull();
  }

  @Test
  @DisplayName("Should handle empty roles set")
  void shouldHandleEmptyRolesSet() throws Exception {
    // Arrange
    var jwt = createJwt();
    var jwtToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(jwtToken);

    var userContext = new UserContext(
        1L,
        "johndoe",
        "john@example.com",
        Set.of(),
        Set.of(),
        null,
        null,
        Map.of()
    );

    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Capture MDC values during filter execution
    doAnswer(invocation -> {
      assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ROLES)).isEqualTo("");
      return null;
    }).when(filterChain).doFilter(request, response);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  private Jwt createJwt() {
    return new Jwt(
        "token",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        Map.of("alg", "RS256"),
        Map.of("sub", "1", "username", "johndoe")
    );
  }
}
