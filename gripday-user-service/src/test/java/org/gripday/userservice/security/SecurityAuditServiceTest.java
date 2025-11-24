package org.gripday.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.gripday.userservice.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

  @Mock
  private UserAuditLogRepository auditLogRepository;

  @InjectMocks
  private SecurityAuditService securityAuditService;

  private static final String TEST_TENANT = "test-tenant";
  private static final String TEST_IP = "192.168.1.1";
  private static final String TEST_USER_AGENT = "Mozilla/5.0";

  @BeforeEach
  void setUp() {
    TenantContext.setCurrentTenantId(TEST_TENANT);
    MDC.put("correlationId", "test-correlation-id");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    MDC.clear();
  }

  @Test
  @DisplayName("logSuccessfulAuthentication creates audit log with correct details")
  void logSuccessfulAuthentication() {
    var username = "testuser";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logSuccessfulAuthentication(username, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("AUTHENTICATION_SUCCESS");
    assertThat(auditLog.getIpAddress()).isEqualTo(TEST_IP);
    assertThat(auditLog.getUserAgent()).isEqualTo(TEST_USER_AGENT);
    assertThat(auditLog.getTenantId()).isEqualTo(TEST_TENANT);
    assertThat(auditLog.getDetails()).contains(username, TEST_IP, TEST_USER_AGENT);
  }

  @Test
  @DisplayName("logFailedAuthentication creates audit log with failure reason")
  void logFailedAuthentication() {
    var username = "testuser";
    var reason = "Invalid password";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logFailedAuthentication(username, reason, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("AUTHENTICATION_FAILURE");
    assertThat(auditLog.getDetails()).contains(username, reason, TEST_IP);
  }

  @Test
  @DisplayName("logAccountLockout creates audit log with failed attempts count")
  void logAccountLockout() {
    var username = "testuser";
    var failedAttempts = 5;
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logAccountLockout(username, failedAttempts, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("ACCOUNT_LOCKOUT");
    assertThat(auditLog.getDetails()).contains(username, String.valueOf(failedAttempts), "15 minutes");
  }

  @Test
  @DisplayName("logPasswordChange creates audit log")
  void logPasswordChange() {
    var username = "testuser";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logPasswordChange(username, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("PASSWORD_CHANGE");
    assertThat(auditLog.getDetails()).contains(username, "Password changed");
  }

  @Test
  @DisplayName("logUserRegistration creates audit log with email")
  void logUserRegistration() {
    var username = "testuser";
    var email = "test@example.com";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logUserRegistration(username, email, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("USER_REGISTRATION");
    assertThat(auditLog.getDetails()).contains(username, email);
  }

  @Test
  @DisplayName("logRateLimitExceeded creates audit log with endpoint")
  void logRateLimitExceeded() {
    var endpoint = "/api/v1/auth/login";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logRateLimitExceeded(TEST_IP, TEST_USER_AGENT, endpoint);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("RATE_LIMIT_EXCEEDED");
    assertThat(auditLog.getDetails()).contains(TEST_IP, endpoint);
  }

  @Test
  @DisplayName("logSuspiciousActivity creates audit log with activity description")
  void logSuspiciousActivity() {
    var username = "testuser";
    var activity = "SQL injection attempt detected";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logSuspiciousActivity(username, activity, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("SUSPICIOUS_ACTIVITY");
    assertThat(auditLog.getDetails()).contains(username, activity);
  }

  @Test
  @DisplayName("logTokenEvent creates audit log for token actions")
  void logTokenEvent() {
    var username = "testuser";
    var action = "issued";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logTokenEvent(username, action, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getAction()).isEqualTo("TOKEN_ISSUED");
    assertThat(auditLog.getDetails()).contains(username, "JWT Token issued");
  }

  @Test
  @DisplayName("audit logging continues even when repository throws exception")
  void auditLoggingExceptionHandling() {
    var username = "testuser";
    when(auditLogRepository.save(any(UserAuditLog.class)))
        .thenThrow(new RuntimeException("Database error"));

    // Should not throw exception
    securityAuditService.logSuccessfulAuthentication(username, TEST_IP, TEST_USER_AGENT);

    verify(auditLogRepository).save(any(UserAuditLog.class));
  }

  @Test
  @DisplayName("audit logs use default tenant when no tenant context set")
  void auditLogsWithDefaultTenant() {
    TenantContext.clear();
    var username = "testuser";
    when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(i -> i.getArgument(0));

    securityAuditService.logSuccessfulAuthentication(username, TEST_IP, TEST_USER_AGENT);

    var captor = ArgumentCaptor.forClass(UserAuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    var auditLog = captor.getValue();
    assertThat(auditLog.getTenantId()).isEqualTo("default");
  }
}
