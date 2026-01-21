package com.iqscaffold.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AccountLockoutServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private AccountLockoutServiceImpl accountLockoutService;

  @BeforeEach
  void setUp() {
    // Setup is done per test as needed
  }

  @Test
  @DisplayName("recordFailedAttempt returns false when under threshold")
  void recordFailedAttemptUnderThreshold() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenReturn(null);
    when(valueOperations.increment(anyString())).thenReturn(2L);

    var shouldLock = accountLockoutService.recordFailedAttempt(username);

    assertThat(shouldLock).isFalse();
    verify(valueOperations).increment("failed_attempts:" + username);
    verify(redisTemplate).expire(eq("failed_attempts:" + username), anyLong(), eq(TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("recordFailedAttempt returns true when threshold reached")
  void recordFailedAttemptThresholdReached() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenReturn(null);
    when(valueOperations.increment(anyString())).thenReturn(5L);

    var shouldLock = accountLockoutService.recordFailedAttempt(username);

    assertThat(shouldLock).isTrue();
    verify(valueOperations).set(eq("lockout:" + username), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    verify(redisTemplate).delete("failed_attempts:" + username);
  }

  @Test
  @DisplayName("recordFailedAttempt returns true when account already locked")
  void recordFailedAttemptAlreadyLocked() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    var futureExpiry = String.valueOf(Instant.now().plusSeconds(600).toEpochMilli());
    when(valueOperations.get("lockout:" + username)).thenReturn(futureExpiry);

    var shouldLock = accountLockoutService.recordFailedAttempt(username);

    assertThat(shouldLock).isTrue();
  }

  @Test
  @DisplayName("recordFailedAttempt returns false when increment returns null")
  void recordFailedAttemptNullIncrement() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenReturn(null);
    when(valueOperations.increment(anyString())).thenReturn(null);

    var shouldLock = accountLockoutService.recordFailedAttempt(username);

    assertThat(shouldLock).isFalse();
  }

  @Test
  @DisplayName("recordFailedAttempt returns false on Redis exception")
  void recordFailedAttemptException() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

    var shouldLock = accountLockoutService.recordFailedAttempt(username);

    assertThat(shouldLock).isFalse();
  }

  @Test
  @DisplayName("isAccountLocked returns true when account is locked")
  void isAccountLockedTrue() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    var futureExpiry = String.valueOf(Instant.now().plusSeconds(600).toEpochMilli());
    when(valueOperations.get("lockout:" + username)).thenReturn(futureExpiry);

    var isLocked = accountLockoutService.isAccountLocked(username);

    assertThat(isLocked).isTrue();
  }

  @Test
  @DisplayName("isAccountLocked returns false when no lockout exists")
  void isAccountLockedFalse() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get("lockout:" + username)).thenReturn(null);

    var isLocked = accountLockoutService.isAccountLocked(username);

    assertThat(isLocked).isFalse();
  }

  @Test
  @DisplayName("isAccountLocked returns false when lockout expired")
  void isAccountLockedExpired() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    var pastExpiry = String.valueOf(Instant.now().minusSeconds(60).toEpochMilli());
    when(valueOperations.get("lockout:" + username)).thenReturn(pastExpiry);

    var isLocked = accountLockoutService.isAccountLocked(username);

    assertThat(isLocked).isFalse();
    verify(redisTemplate).delete("lockout:" + username);
  }

  @Test
  @DisplayName("isAccountLocked returns false on exception")
  void isAccountLockedException() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

    var isLocked = accountLockoutService.isAccountLocked(username);

    assertThat(isLocked).isFalse();
  }

  @Test
  @DisplayName("clearFailedAttempts deletes the key")
  void clearFailedAttempts() {
    var username = "testuser";

    accountLockoutService.clearFailedAttempts(username);

    verify(redisTemplate).delete("failed_attempts:" + username);
  }

  @Test
  @DisplayName("getFailedAttempts returns correct count")
  void getFailedAttempts() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get("failed_attempts:" + username)).thenReturn("3");

    var count = accountLockoutService.getFailedAttempts(username);

    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("getFailedAttempts returns zero when no attempts")
  void getFailedAttemptsZero() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get("failed_attempts:" + username)).thenReturn(null);

    var count = accountLockoutService.getFailedAttempts(username);

    assertThat(count).isZero();
  }

  @Test
  @DisplayName("getFailedAttempts returns zero on exception")
  void getFailedAttemptsException() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

    var count = accountLockoutService.getFailedAttempts(username);

    assertThat(count).isZero();
  }

  @Test
  @DisplayName("getTimeUntilUnlock returns correct duration")
  void getTimeUntilUnlock() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    var futureExpiry = String.valueOf(Instant.now().plusSeconds(300).toEpochMilli());
    when(valueOperations.get("lockout:" + username)).thenReturn(futureExpiry);

    var duration = accountLockoutService.getTimeUntilUnlock(username);

    assertThat(duration).isNotNull();
    assertThat(duration.toSeconds()).isGreaterThan(0);
    assertThat(duration.toSeconds()).isLessThanOrEqualTo(300);
  }

  @Test
  @DisplayName("getTimeUntilUnlock returns zero when not locked")
  void getTimeUntilUnlockNotLocked() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get("lockout:" + username)).thenReturn(null);

    var duration = accountLockoutService.getTimeUntilUnlock(username);

    assertThat(duration).isEqualTo(Duration.ZERO);
  }

  @Test
  @DisplayName("getTimeUntilUnlock returns zero on exception")
  void getTimeUntilUnlockException() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "testuser";
    when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

    var duration = accountLockoutService.getTimeUntilUnlock(username);

    assertThat(duration).isEqualTo(Duration.ZERO);
  }

  @Test
  @DisplayName("unlockAccount deletes both keys")
  void unlockAccount() {
    var username = "testuser";

    accountLockoutService.unlockAccount(username);

    verify(redisTemplate).delete("failed_attempts:" + username);
    verify(redisTemplate).delete("lockout:" + username);
  }

  @Test
  @DisplayName("username is normalized to lowercase")
  void usernameNormalization() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    var username = "TestUser";
    when(valueOperations.get(anyString())).thenReturn(null);
    when(valueOperations.increment(anyString())).thenReturn(1L);

    accountLockoutService.recordFailedAttempt(username);

    verify(valueOperations).increment("failed_attempts:testuser");
  }
}
