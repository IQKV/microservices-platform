package org.gripday.userservice.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.gripday.userservice.shared.UserServiceConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for TenantExtractionService.
 */
@ExtendWith(MockitoExtension.class)
class TenantExtractionServiceTest {

  @Mock
  private TenantManagementService tenantManagementService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private Authentication authentication;

  @Mock
  private Jwt jwt;

  @InjectMocks
  private TenantExtractionService service;

  @BeforeEach
  void setUp() {
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should extract tenant from JWT token")
  void shouldExtractTenantFromJwt() {
    // Arrange
    var tenantId = "tenant-123";
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaimAsString(UserServiceConstants.JwtClaims.TENANT_ID)).thenReturn(tenantId);
    when(tenantManagementService.isValidTenant(tenantId)).thenReturn(true);

    // Act
    var result = service.extractTenantFromRequest(request, authentication);

    // Assert
    assertThat(result.isValid()).isTrue();
    assertThat(result.tenantId()).isEqualTo(tenantId);
    assertThat(result.resolutionMethod()).isEqualTo("JWT");
  }

  @Test
  @DisplayName("Should extract tenant from header when JWT is not available")
  void shouldExtractTenantFromHeader() {
    // Arrange
    var tenantId = "tenant-456";
    when(request.getHeader(UserServiceConstants.Headers.X_TENANT_ID)).thenReturn(tenantId);
    when(tenantManagementService.isValidTenant(tenantId)).thenReturn(true);

    // Act
    var result = service.extractTenantFromRequest(request, null);

    // Assert
    assertThat(result.isValid()).isTrue();
    assertThat(result.tenantId()).isEqualTo(tenantId);
    assertThat(result.resolutionMethod()).isEqualTo("HEADER");
  }

  @Test
  @DisplayName("Should return invalid result when no tenant found")
  void shouldReturnInvalidResultWhenNoTenantFound() {
    // Arrange
    when(request.getHeader(UserServiceConstants.Headers.X_TENANT_ID)).thenReturn(null);

    // Act
    var result = service.extractTenantFromRequest(request, null);

    // Assert
    assertThat(result.isValid()).isFalse();
    assertThat(result.tenantId()).isNull();
    assertThat(result.resolutionMethod()).isEqualTo("NONE");
  }

  @Test
  @DisplayName("Should return invalid result when tenant is not valid")
  void shouldReturnInvalidResultWhenTenantIsNotValid() {
    // Arrange
    var tenantId = "invalid-tenant";
    when(request.getHeader(UserServiceConstants.Headers.X_TENANT_ID)).thenReturn(tenantId);
    when(tenantManagementService.isValidTenant(tenantId)).thenReturn(false);

    // Act
    var result = service.extractTenantFromRequest(request, null);

    // Assert
    assertThat(result.isValid()).isFalse();
  }

  @Test
  @DisplayName("Should extract tenant from JWT with trimmed value")
  void shouldExtractTenantFromJwtWithTrimmedValue() {
    // Arrange
    var tenantId = "  tenant-123  ";
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaimAsString(UserServiceConstants.JwtClaims.TENANT_ID)).thenReturn(tenantId);

    // Act
    var result = service.extractTenantFromJwt(authentication);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should extract tenant from header with trimmed value")
  void shouldExtractTenantFromHeaderWithTrimmedValue() {
    // Arrange
    var tenantId = "  tenant-456  ";
    when(request.getHeader(UserServiceConstants.Headers.X_TENANT_ID)).thenReturn(tenantId);

    // Act
    var result = service.extractTenantFromHeader(request);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo("tenant-456");
  }

  @Test
  @DisplayName("Should return empty when JWT claim is empty")
  void shouldReturnEmptyWhenJwtClaimIsEmpty() {
    // Arrange
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaimAsString(UserServiceConstants.JwtClaims.TENANT_ID)).thenReturn("");

    // Act
    var result = service.extractTenantFromJwt(authentication);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when header is empty")
  void shouldReturnEmptyWhenHeaderIsEmpty() {
    // Arrange
    when(request.getHeader(UserServiceConstants.Headers.X_TENANT_ID)).thenReturn("");

    // Act
    var result = service.extractTenantFromHeader(request);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should set tenant context from resolution result")
  void shouldSetTenantContextFromResolutionResult() {
    // Arrange
    var tenantId = "tenant-123";
    var resolutionResult = new org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantResolutionResult(
        tenantId, "JWT", tenantId, true
    );

    // Act
    service.setTenantContext(resolutionResult);

    // Assert
    assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
  }

  @Test
  @DisplayName("Should not set tenant context when result is invalid")
  void shouldNotSetTenantContextWhenResultIsInvalid() {
    // Arrange
    var resolutionResult = new org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantResolutionResult(
        null, "NONE", null, false
    );

    // Act
    service.setTenantContext(resolutionResult);

    // Assert
    assertThat(TenantContext.getCurrentTenantId()).isNull();
  }

  @Test
  @DisplayName("Should extract and set tenant context successfully")
  void shouldExtractAndSetTenantContextSuccessfully() {
    // Arrange
    var tenantId = "tenant-123";
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaimAsString(UserServiceConstants.JwtClaims.TENANT_ID)).thenReturn(tenantId);
    when(tenantManagementService.isValidTenant(tenantId)).thenReturn(true);

    // Act
    var result = service.extractAndSetTenantContext(request, authentication);

    // Assert
    assertThat(result).isTrue();
    assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
  }

  @Test
  @DisplayName("Should return false when extract and set fails")
  void shouldReturnFalseWhenExtractAndSetFails() {
    // Arrange
    when(request.getHeader(UserServiceConstants.Headers.X_TENANT_ID)).thenReturn(null);

    // Act
    var result = service.extractAndSetTenantContext(request, null);

    // Assert
    assertThat(result).isFalse();
    assertThat(TenantContext.getCurrentTenantId()).isNull();
  }

  @Test
  @DisplayName("Should return tenant header name")
  void shouldReturnTenantHeaderName() {
    // Act
    var headerName = TenantExtractionService.getTenantHeaderName();

    // Assert
    assertThat(headerName).isEqualTo(UserServiceConstants.Headers.X_TENANT_ID);
  }

  @Test
  @DisplayName("Should return tenant JWT claim name")
  void shouldReturnTenantJwtClaimName() {
    // Act
    var claimName = TenantExtractionService.getTenantJwtClaim();

    // Assert
    assertThat(claimName).isEqualTo(UserServiceConstants.JwtClaims.TENANT_ID);
  }
}
