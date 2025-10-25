package org.gripday.authservice.unit;

import org.gripday.authservice.domain.service.EmailVerificationMetricsService;
import org.gripday.authservice.domain.service.EmailVerificationTokenCleanupService;
import org.gripday.authservice.infrastructure.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailVerificationTokenCleanupService.
 * Tests scheduled cleanup functionality and metrics recording.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenCleanupServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailVerificationMetricsService metricsService;

    private EmailVerificationTokenCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new EmailVerificationTokenCleanupService(tokenRepository, metricsService);
    }

    @Test
    void shouldPerformScheduledCleanupSuccessfully() {
        // Given
        var expiredCount = 5L;
        var deletedCount = 3;
        var timerSample = mock(io.micrometer.core.instrument.Timer.Sample.class);
        
        when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(expiredCount);
        when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(deletedCount);
        when(metricsService.startCleanupTimer()).thenReturn(timerSample);

        // When
        cleanupService.cleanupExpiredTokens();

        // Then
        var cutoffTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenRepository).countByExpiresAtBefore(cutoffTimeCaptor.capture());
        verify(tokenRepository).deleteByExpiresAtBefore(cutoffTimeCaptor.capture());
        
        // Verify cutoff time is approximately 48 hours ago
        var cutoffTime = cutoffTimeCaptor.getValue();
        var expectedCutoff = LocalDateTime.now().minusHours(48);
        assertThat(cutoffTime).isBetween(expectedCutoff.minusMinutes(1), expectedCutoff.plusMinutes(1));
        
        // Verify metrics are recorded
        verify(metricsService).updateExpiredTokensCount(expiredCount);
        verify(metricsService).recordTokensCleanedUp(deletedCount);
        verify(metricsService).startCleanupTimer();
    }

    @Test
    void shouldHandleCleanupErrorsGracefully() {
        // Given
        var timerSample = mock(io.micrometer.core.instrument.Timer.Sample.class);
        
        when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        when(metricsService.startCleanupTimer()).thenReturn(timerSample);

        // When
        cleanupService.cleanupExpiredTokens();

        // Then - should not throw exception and should not record metrics for failed operation
        verify(metricsService, never()).updateExpiredTokensCount(anyLong());
        verify(metricsService, never()).recordTokensCleanedUp(anyLong());
        verify(metricsService).startCleanupTimer(); // Timer should still be started
    }

    @Test
    void shouldPerformManualCleanupSuccessfully() {
        // Given
        var expiredCount = 10L;
        var deletedCount = 8;
        var timerSample = mock(io.micrometer.core.instrument.Timer.Sample.class);
        
        when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(expiredCount);
        when(tokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(deletedCount);
        when(metricsService.startCleanupTimer()).thenReturn(timerSample);

        // When
        var result = cleanupService.performManualCleanup();

        // Then
        assertThat(result).isEqualTo(deletedCount);
        
        var cutoffTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenRepository).countByExpiresAtBefore(cutoffTimeCaptor.capture());
        verify(tokenRepository).deleteByExpiresAtBefore(cutoffTimeCaptor.capture());
        
        // Verify cutoff time is approximately 48 hours ago
        var cutoffTime = cutoffTimeCaptor.getValue();
        var expectedCutoff = LocalDateTime.now().minusHours(48);
        assertThat(cutoffTime).isBetween(expectedCutoff.minusMinutes(1), expectedCutoff.plusMinutes(1));
        
        // Verify metrics are recorded
        verify(metricsService).updateExpiredTokensCount(expiredCount);
        verify(metricsService).recordTokensCleanedUp(deletedCount);
        verify(metricsService).startCleanupTimer();
    }

    @Test
    void shouldThrowExceptionOnManualCleanupError() {
        // Given
        var timerSample = mock(io.micrometer.core.instrument.Timer.Sample.class);
        
        when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        when(metricsService.startCleanupTimer()).thenReturn(timerSample);

        // When & Then
        try {
            cleanupService.performManualCleanup();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Failed to perform manual cleanup");
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Database error");
        }
        
        // Verify metrics are not recorded for failed operation
        verify(metricsService, never()).updateExpiredTokensCount(anyLong());
        verify(metricsService, never()).recordTokensCleanedUp(anyLong());
        verify(metricsService).startCleanupTimer(); // Timer should still be started
    }

    @Test
    void shouldGetExpiredTokenCountSuccessfully() {
        // Given
        var expiredCount = 15L;
        when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(expiredCount);

        // When
        var result = cleanupService.getExpiredTokenCount();

        // Then
        assertThat(result).isEqualTo(expiredCount);
        
        var cutoffTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenRepository).countByExpiresAtBefore(cutoffTimeCaptor.capture());
        
        // Verify cutoff time is approximately 48 hours ago
        var cutoffTime = cutoffTimeCaptor.getValue();
        var expectedCutoff = LocalDateTime.now().minusHours(48);
        assertThat(cutoffTime).isBetween(expectedCutoff.minusMinutes(1), expectedCutoff.plusMinutes(1));
    }

    @Test
    void shouldReturnZeroOnExpiredTokenCountError() {
        // Given
        when(tokenRepository.countByExpiresAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When
        var result = cleanupService.getExpiredTokenCount();

        // Then
        assertThat(result).isEqualTo(0);
    }
}