package org.gripday.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAuditLogEntityTest {

  @Test
  @DisplayName("action helpers categorize correctly")
  void actionCategories() {
    var log1 = new UserAuditLog(1L, "LOGIN_SUCCESS", "t1");
    var log2 = new UserAuditLog(1L, "PASSWORD_CHANGE", "t1");
    var log3 = new UserAuditLog(1L, "ROLE_UPDATED", "t1");
    var log4 = new UserAuditLog(1L, "SOMETHING", "t1");

    assertThat(log1.isLoginAction()).isTrue();
    assertThat(log2.isSecurityAction()).isTrue();
    assertThat(log3.getActionCategory()).isEqualTo("AUTHORIZATION");
    assertThat(log4.getActionCategory()).isEqualTo("GENERAL");
  }

  @Test
  @DisplayName("tenant and basic fields")
  void tenantAndFields() {
    var log = new UserAuditLog(42L, "LOGIN_FAILURE", "details", "1.2.3.4", "agent", "TEN");
    assertThat(log.getTenantId()).isEqualTo("TEN");
    assertThat(log.getUserId()).isEqualTo(42L);
    assertThat(log.getAction()).isEqualTo("LOGIN_FAILURE");

    log.setCreatedAt(Instant.now());

    assertThat(log.getDetails()).isEqualTo("details");
    assertThat(log.getIpAddress()).isEqualTo("1.2.3.4");
    assertThat(log.getUserAgent()).isEqualTo("agent");
  }
}
