package com.iqscaffold.billingservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class SecurityContextHelperTest {

  @Mock
  private HttpServletRequest request;

  private UserContext testUserContext;

  @BeforeEach
  void setUp() {
    testUserContext = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("READ", "WRITE"),
        "tenant1",
        null,
        "John",
        "Doe"
    );
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void getCurrentUserContext_shouldReturnUserContextWhenPresent() {
    // Given
    when(request.getAttribute("userContext")).thenReturn(testUserContext);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    UserContext result = SecurityContextHelper.getCurrentUserContext();

    // Then
    assertNotNull(result);
    assertEquals(testUserContext, result);
    assertEquals(1L, result.userId());
    assertEquals("testuser", result.username());
  }

  @Test
  void getCurrentUserContext_shouldReturnNullWhenAttributesNotSet() {
    // Given
    RequestContextHolder.resetRequestAttributes();

    // When
    UserContext result = SecurityContextHelper.getCurrentUserContext();

    // Then
    assertNull(result);
  }

  @Test
  void getCurrentUserContext_shouldReturnNullWhenUserContextNotInRequest() {
    // Given
    when(request.getAttribute("userContext")).thenReturn(null);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    UserContext result = SecurityContextHelper.getCurrentUserContext();

    // Then
    assertNull(result);
  }

  @Test
  void getCurrentUserContext_shouldReturnNullWhenAttributeIsNotUserContext() {
    // Given
    when(request.getAttribute("userContext")).thenReturn("not a user context");
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    UserContext result = SecurityContextHelper.getCurrentUserContext();

    // Then
    assertNull(result);
  }

  @Test
  void getCurrentUserContextOrThrow_shouldReturnUserContextWhenPresent() {
    // Given
    when(request.getAttribute("userContext")).thenReturn(testUserContext);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    UserContext result = SecurityContextHelper.getCurrentUserContextOrThrow();

    // Then
    assertNotNull(result);
    assertEquals(testUserContext, result);
  }

  @Test
  void getCurrentUserContextOrThrow_shouldThrowExceptionWhenContextNotFound() {
    // Given
    RequestContextHolder.resetRequestAttributes();

    // When & Then
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        SecurityContextHelper::getCurrentUserContextOrThrow
    );

    assertTrue(exception.getMessage().contains("User context not found"));
  }

  @Test
  void getCurrentUserContextOrThrow_shouldThrowExceptionWhenAttributesNull() {
    // Given
    when(request.getAttribute("userContext")).thenReturn(null);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When & Then
    assertThrows(
        IllegalStateException.class,
        SecurityContextHelper::getCurrentUserContextOrThrow
    );
  }

  @Test
  void getCurrentUserId_shouldReturnUserIdWhenContextPresent() {
    // Given
    when(request.getAttribute("userContext")).thenReturn(testUserContext);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    Long result = SecurityContextHelper.getCurrentUserId();

    // Then
    assertNotNull(result);
    assertEquals(1L, result);
  }

  @Test
  void getCurrentUserId_shouldReturnNullWhenContextNotPresent() {
    // Given
    RequestContextHolder.resetRequestAttributes();

    // When
    Long result = SecurityContextHelper.getCurrentUserId();

    // Then
    assertNull(result);
  }

  @Test
  void getCurrentTenantId_shouldReturnTenantIdWhenContextPresent() {
    // Given
    when(request.getAttribute("userContext")).thenReturn(testUserContext);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    String result = SecurityContextHelper.getCurrentTenantId();

    // Then
    assertNotNull(result);
    assertEquals("tenant1", result);
  }

  @Test
  void getCurrentTenantId_shouldReturnNullWhenContextNotPresent() {
    // Given
    RequestContextHolder.resetRequestAttributes();

    // When
    String result = SecurityContextHelper.getCurrentTenantId();

    // Then
    assertNull(result);
  }

  @Test
  void getCurrentUserId_shouldReturnNullWhenUserIdIsNull() {
    // Given
    UserContext contextWithNullUserId = new UserContext(
        null, "user", "email@test.com", Set.of(), "tenant1", null, "John", "Doe"
    );
    when(request.getAttribute("userContext")).thenReturn(contextWithNullUserId);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    Long result = SecurityContextHelper.getCurrentUserId();

    // Then
    assertNull(result);
  }

  @Test
  void getCurrentTenantId_shouldReturnNullWhenTenantIdIsNull() {
    // Given
    UserContext contextWithNullTenantId = new UserContext(
        1L, "user", "email@test.com", Set.of(), null, null, "John", "Doe"
    );
    when(request.getAttribute("userContext")).thenReturn(contextWithNullTenantId);
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // When
    String result = SecurityContextHelper.getCurrentTenantId();

    // Then
    assertNull(result);
  }
}
