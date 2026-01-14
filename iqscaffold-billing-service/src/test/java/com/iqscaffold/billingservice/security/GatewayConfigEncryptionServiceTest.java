package com.iqscaffold.billingservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayConfigEncryptionServiceTest {

  private GatewayConfigEncryptionService encryptionService;
  private IqScaffoldProperties properties;

  @BeforeEach
  void setUp() {
    properties = mock(IqScaffoldProperties.class);
    IqScaffoldProperties.Billing billing = mock(IqScaffoldProperties.Billing.class);
    IqScaffoldProperties.Billing.Security security = mock(IqScaffoldProperties.Billing.Security.class);
    IqScaffoldProperties.Billing.Security.Encryption encryption = mock(IqScaffoldProperties.Billing.Security.Encryption.class);

    when(properties.billing()).thenReturn(billing);
    when(billing.security()).thenReturn(security);
    when(security.encryption()).thenReturn(encryption);
    when(encryption.masterKey()).thenReturn("test-master-key-for-encryption-12345678");

    encryptionService = new GatewayConfigEncryptionService(properties);
  }

  @Test
  void encrypt_shouldEncryptPlaintext() {
    // Given
    String plaintext = "{\"apiKey\":\"sk_test_123\",\"webhookSecret\":\"whsec_456\"}";
    String tenantId = "tenant-123";

    // When
    String encrypted = encryptionService.encrypt(plaintext, tenantId);

    // Then
    assertNotNull(encrypted);
    assertNotEquals(plaintext, encrypted);
  }

  @Test
  void decrypt_shouldDecryptCiphertext() {
    // Given
    String plaintext = "{\"apiKey\":\"sk_test_123\",\"webhookSecret\":\"whsec_456\"}";
    String tenantId = "tenant-123";
    String encrypted = encryptionService.encrypt(plaintext, tenantId);

    // When
    String decrypted = encryptionService.decrypt(encrypted, tenantId);

    // Then
    assertEquals(plaintext, decrypted);
  }

  @Test
  void encrypt_shouldProduceDifferentCiphertextForSameInput() {
    // Given
    String plaintext = "test data";
    String tenantId = "tenant-123";

    // When
    String encrypted1 = encryptionService.encrypt(plaintext, tenantId);
    String encrypted2 = encryptionService.encrypt(plaintext, tenantId);

    // Then - Different due to unique IV per encryption
    assertNotEquals(encrypted1, encrypted2);
  }

  @Test
  void encrypt_shouldProduceDifferentCiphertextForDifferentTenants() {
    // Given
    String plaintext = "test data";

    // When
    String encrypted1 = encryptionService.encrypt(plaintext, "tenant-1");
    String encrypted2 = encryptionService.encrypt(plaintext, "tenant-2");

    // Then
    assertNotEquals(encrypted1, encrypted2);
  }

  @Test
  void decrypt_shouldFailWithWrongTenantId() {
    // Given
    String plaintext = "test data";
    String encrypted = encryptionService.encrypt(plaintext, "tenant-1");

    // When & Then
    assertThrows(RuntimeException.class, () ->
        encryptionService.decrypt(encrypted, "tenant-2")
    );
  }

  @Test
  void encrypt_shouldThrowExceptionForNullPlaintext() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        encryptionService.encrypt(null, "tenant-123")
    );
  }

  @Test
  void encrypt_shouldThrowExceptionForEmptyPlaintext() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        encryptionService.encrypt("", "tenant-123")
    );
  }

  @Test
  void encrypt_shouldThrowExceptionForNullTenantId() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        encryptionService.encrypt("test", null)
    );
  }

  @Test
  void decrypt_shouldThrowExceptionForNullCiphertext() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        encryptionService.decrypt(null, "tenant-123")
    );
  }

  @Test
  void decrypt_shouldThrowExceptionForEmptyCiphertext() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        encryptionService.decrypt("", "tenant-123")
    );
  }

  @Test
  void decrypt_shouldThrowExceptionForNullTenantId() {
    // Given
    String encrypted = encryptionService.encrypt("test", "tenant-123");

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        encryptionService.decrypt(encrypted, null)
    );
  }

  @Test
  void decrypt_shouldThrowExceptionForInvalidCiphertext() {
    // When & Then
    assertThrows(RuntimeException.class, () ->
        encryptionService.decrypt("invalid-base64-data", "tenant-123")
    );
  }

  @Test
  void maskSensitiveData_shouldMaskLongString() {
    // Given
    String sensitive = "sk_test_1234567890";

    // When
    String masked = encryptionService.maskSensitiveData(sensitive);

    // Then
    assertEquals("****7890", masked);
  }

  @Test
  void maskSensitiveData_shouldReturnStarsForShortString() {
    // Given
    String sensitive = "abc";

    // When
    String masked = encryptionService.maskSensitiveData(sensitive);

    // Then
    assertEquals("****", masked);
  }

  @Test
  void maskSensitiveData_shouldReturnStarsForNull() {
    // When
    String masked = encryptionService.maskSensitiveData(null);

    // Then
    assertEquals("****", masked);
  }

  @Test
  void maskSensitiveData_shouldReturnStarsForEmpty() {
    // When
    String masked = encryptionService.maskSensitiveData("");

    // Then
    assertEquals("****", masked);
  }

  @Test
  void getLastFourChars_shouldReturnLastFourCharacters() {
    // Given
    String sensitive = "sk_test_1234567890";

    // When
    String lastFour = encryptionService.getLastFourChars(sensitive);

    // Then
    assertEquals("7890", lastFour);
  }

  @Test
  void getLastFourChars_shouldReturnFullStringIfShorterThanFour() {
    // Given
    String sensitive = "abc";

    // When
    String lastFour = encryptionService.getLastFourChars(sensitive);

    // Then
    assertEquals("abc", lastFour);
  }

  @Test
  void getLastFourChars_shouldReturnEmptyForNull() {
    // When
    String lastFour = encryptionService.getLastFourChars(null);

    // Then
    assertEquals("", lastFour);
  }

  @Test
  void getLastFourChars_shouldReturnEmptyForEmptyString() {
    // When
    String lastFour = encryptionService.getLastFourChars("");

    // Then
    assertEquals("", lastFour);
  }
}
