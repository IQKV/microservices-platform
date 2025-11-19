package org.gripday.userservice.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationTokenEntityTest {

  @Test
  @DisplayName("isExpired and isValid reflect expiry and used flags")
  void expiryAndValidity() {
    var future = LocalDateTime.now().plusHours(1);
    var token = new VerificationToken("t", 1L, future, "tenant1");

    assertThat(token.isExpired()).isFalse();
    assertThat(token.isValid()).isTrue();
    assertThat(token.isUnused()).isTrue();

    token.markAsUsed();
    assertThat(token.getUsed()).isTrue();
    assertThat(token.isValid()).isFalse();
    assertThat(token.isUnused()).isFalse();

    var past = LocalDateTime.now().minusMinutes(1);
    var expired = new VerificationToken("t2", 1L, past, "tenant1");
    assertThat(expired.isExpired()).isTrue();
    assertThat(expired.isValid()).isFalse();
  }
}
