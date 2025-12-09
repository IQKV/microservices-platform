package com.iqscaffold.billingservice.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantExtractionServiceTest {

  @Mock
  private HttpServletRequest request;

  private TenantExtractionService tenantExtractionService;

  @BeforeEach
  void setUp() {
    tenantExtractionService = new TenantExtractionService();
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should extract tenant ID from X-Tenant-ID header")
  void shouldExtractTenantFromHeader() {
    // Arrange
    var tenantId = "tenant-123";
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn(tenantId);

    // Act
    var result = tenantExtractionService.extractTenantFromRequest(request);

    // Assert
    assertThat(result).isEqualTo(tenantId);
  }

  @Test
  @DisplayName("Should trim tenant ID from header")
  void shouldTrimTenantIdFromHeader() {
    // Arrange
    var tenantId = "  tenant-456  ";
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn(tenantId);

    // Act
    var result = tenantExtractionService.extractTenantFromRequest(request);

    // Assert
    assertThat(result).isEqualTo("tenant-456");
  }

  @Test
  @DisplayName("Should return null when no tenant header present")
  void shouldReturnNullWhenNoTenantHeader() {
    // Arrange
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn(null);

    // Act
    var result = tenantExtractionService.extractTenantFromRequest(request);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when tenant header is empty")
  void shouldReturnNullWhenTenantHeaderIsEmpty() {
    // Arrange
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn("   ");

    // Act
    var result = tenantExtractionService.extractTenantFromRequest(request);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should truncate tenant ID to 100 characters")
  void shouldTruncateTenantIdTo100Characters() {
    // Arrange
    var longTenantId = "a".repeat(150);
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn(longTenantId);

    // Act
    var result = tenantExtractionService.extractTenantFromRequest(request);

    // Assert
    assertThat(result).hasSize(100);
    assertThat(result).isEqualTo("a".repeat(100));
  }

  @Test
  @DisplayName("Should extract and set tenant context successfully")
  void shouldExtractAndSetTenantContext() {
    // Arrange
    var tenantId = "tenant-789";
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn(tenantId);

    // Act
    var result = tenantExtractionService.extractAndSetTenantContext(request);

    // Assert
    assertThat(result).isTrue();
    assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
  }

  @Test
  @DisplayName("Should return false when tenant context cannot be set")
  void shouldReturnFalseWhenTenantContextCannotBeSet() {
    // Arrange
    when(request.getHeader(BillingConstants.Headers.TENANT_ID)).thenReturn(null);

    // Act
    var result = tenantExtractionService.extractAndSetTenantContext(request);

    // Assert
    assertThat(result).isFalse();
    assertThat(TenantContext.getCurrentTenantId()).isNull();
  }
}
