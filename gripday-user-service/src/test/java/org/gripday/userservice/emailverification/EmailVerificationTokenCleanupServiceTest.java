package org.gripday.userservice.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import io.micrometer.core.instrument.Timer;
import org.gripday.userservice.tenancy.Tenant;
import org.gripday.userservice.tenancy.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EmailVerificationTokenCleanupService.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenCleanupServiceTest {

  @Mock
  private VerificationTokenRepository tokenRepository;

  @Mock
  private VerificationMetrics metricsService;

  @Mock
  private TenantRepository tenantRepository;

  @Mock
  private Timer.Sample timerSample;

  @Mock
  private Timer timer;

  private EmailVerificationTokenCleanupService service;

  @BeforeEach
  void setUp() {
    service = new EmailVerificationTokenCleanupService(
        tokenRepository,
        metricsService,
        tenantRepository
    );

    lenient().when(metricsService.startCleanupTimer()).thenReturn(timerSample);
    lenient().when(metricsService.getCleanupTimer()).thenReturn(timer);
  }

  @Test
  @DisplayName("Should cleanup expired tokens successfully")
  void shouldCleanupExpiredTokensSuccessfully() {
    // Arrange
    var tenant1 = new Tenant("tenant-1", "Tenant 1");
    tenant1.setEnabled(true);
    var tenant2 = new Tenant("tenant-2", "Tenant 2");
    tenant2.setEnabled(true);
    
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant1, tenant2));
    when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5L, 3L);
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5, 3);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(tenantRepository).findByEnabledTrue();
    verify(metricsService).startCleanupTimer();
    verify(timerSample).stop(timer);
  }

  @Test
  @DisplayName("Should handle cleanup errors gracefully")
  void shouldHandleCleanupErrorsGracefully() {
    // Arrange
    when(tenantRepository.findByEnabledTrue()).thenThrow(new RuntimeException("Database error"));

    // Act - should not throw exception
    service.cleanupExpiredTokens();

    // Assert - method completes without exception
    verify(timerSample).stop(timer);
  }

  @Test
  @DisplayName("Should perform manual cleanup successfully")
  void shouldPerformManualCleanupSuccessfully() {
    // Arrange
    var tenant1 = new Tenant("tenant-1", "Tenant 1");
    tenant1.setEnabled(true);
    
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant1));
    when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5L);
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5);

    // Act
    var deletedCount = service.performManualCleanup();

    // Assert
    assertThat(deletedCount).isEqualTo(5L);
    verify(tenantRepository).findByEnabledTrue();
    verify(metricsService).startCleanupTimer();
    verify(timerSample).stop(timer);
  }

  @Test
  @DisplayName("Should throw exception when manual cleanup fails")
  void shouldThrowExceptionWhenManualCleanupFails() {
    // Arrange
    when(tenantRepository.findByEnabledTrue()).thenThrow(new RuntimeException("Database error"));

    // Act & Assert
    assertThatThrownBy(() -> service.performManualCleanup())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to perform manual cleanup");

    verify(timerSample).stop(timer);
  }

  @Test
  @DisplayName("Should get expired token count")
  void shouldGetExpiredTokenCount() {
    // Arrange
    var tenant1 = new Tenant("tenant-1", "Tenant 1");
    tenant1.setEnabled(true);
    var tenant2 = new Tenant("tenant-2", "Tenant 2");
    tenant2.setEnabled(true);
    
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant1, tenant2));
    when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5L, 3L);

    // Act
    var count = service.getExpiredTokenCount();

    // Assert
    assertThat(count).isEqualTo(8L);
    verify(tenantRepository).findByEnabledTrue();
  }

  @Test
  @DisplayName("Should return zero when counting fails")
  void shouldReturnZeroWhenCountingFails() {
    // Arrange
    when(tenantRepository.findByEnabledTrue()).thenThrow(new RuntimeException("Database error"));

    // Act
    var count = service.getExpiredTokenCount();

    // Assert
    assertThat(count).isEqualTo(0L);
  }

  @Test
  @DisplayName("Should handle empty tenant list")
  void shouldHandleEmptyTenantList() {
    // Arrange
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of());

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(tenantRepository).findByEnabledTrue();
    verify(timerSample).stop(timer);
  }

  @Test
  @DisplayName("Should record metrics during cleanup")
  void shouldRecordMetricsDuringCleanup() {
    // Arrange
    var tenant1 = new Tenant("tenant-1", "Tenant 1");
    tenant1.setEnabled(true);
    
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant1));
    when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5L);
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(5);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(metricsService).startCleanupTimer();
    verify(timerSample).stop(timer);
  }

  @Test
  @DisplayName("Should process multiple tenants during cleanup")
  void shouldProcessMultipleTenantsDuringCleanup() {
    // Arrange
    var tenant1 = new Tenant("tenant-1", "Tenant 1");
    tenant1.setEnabled(true);
    var tenant2 = new Tenant("tenant-2", "Tenant 2");
    tenant2.setEnabled(true);
    var tenant3 = new Tenant("tenant-3", "Tenant 3");
    tenant3.setEnabled(true);
    
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant1, tenant2, tenant3));
    when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(2L, 3L, 1L);
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(2, 3, 1);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(tenantRepository).findByEnabledTrue();
  }

  @Test
  @DisplayName("Should handle zero expired tokens")
  void shouldHandleZeroExpiredTokens() {
    // Arrange
    var tenant1 = new Tenant("tenant-1", "Tenant 1");
    tenant1.setEnabled(true);
    
    when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant1));
    when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(0L);
    when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(0);

    // Act
    var deletedCount = service.performManualCleanup();

    // Assert
    assertThat(deletedCount).isEqualTo(0L);
  }
}
