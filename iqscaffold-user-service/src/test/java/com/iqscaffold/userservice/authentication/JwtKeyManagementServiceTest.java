package com.iqscaffold.userservice.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JwtKeyManagementService.
 */
class JwtKeyManagementServiceTest {

  private JwtKeyManagementService service;

  @BeforeEach
  void setUp() {
    service = new JwtKeyManagementService();
  }

  @Test
  @DisplayName("Should initialize with a key pair on construction")
  void shouldInitializeWithKeyPair() {
    // Assert
    assertThat(service.getCurrentKeyId()).isNotNull();
    assertThat(service.getCurrentKeyPair()).isNotNull();
    assertThat(service.getCurrentKeyPair().getPublic()).isNotNull();
    assertThat(service.getCurrentKeyPair().getPrivate()).isNotNull();
  }

  @Test
  @DisplayName("Should get current key pair")
  void shouldGetCurrentKeyPair() {
    // Act
    var keyPair = service.getCurrentKeyPair();

    // Assert
    assertThat(keyPair).isNotNull();
    assertThat(keyPair.getPublic()).isNotNull();
    assertThat(keyPair.getPrivate()).isNotNull();
  }

  @Test
  @DisplayName("Should get current key ID")
  void shouldGetCurrentKeyId() {
    // Act
    var keyId = service.getCurrentKeyId();

    // Assert
    assertThat(keyId).isNotNull();
    assertThat(keyId).isNotEmpty();
  }

  @Test
  @DisplayName("Should rotate keys successfully")
  void shouldRotateKeys() {
    // Arrange
    var originalKeyId = service.getCurrentKeyId();
    var originalKeyPair = service.getCurrentKeyPair();

    // Act
    service.rotateKeys();

    // Assert
    var newKeyId = service.getCurrentKeyId();
    var newKeyPair = service.getCurrentKeyPair();

    assertThat(newKeyId).isNotEqualTo(originalKeyId);
    assertThat(newKeyPair).isNotEqualTo(originalKeyPair);
    assertThat(newKeyPair.getPublic()).isNotNull();
    assertThat(newKeyPair.getPrivate()).isNotNull();
  }

  @Test
  @DisplayName("Should keep old keys after rotation")
  void shouldKeepOldKeysAfterRotation() {
    // Arrange
    var originalKeyId = service.getCurrentKeyId();

    // Act
    service.rotateKeys();

    // Assert
    var oldKeyPair = service.getKeyPairById(originalKeyId);
    assertThat(oldKeyPair).isNotNull();
  }

  @Test
  @DisplayName("Should get key pair by ID")
  void shouldGetKeyPairById() {
    // Arrange
    var keyId = service.getCurrentKeyId();

    // Act
    var keyPair = service.getKeyPairById(keyId);

    // Assert
    assertThat(keyPair).isNotNull();
    assertThat(keyPair).isEqualTo(service.getCurrentKeyPair());
  }

  @Test
  @DisplayName("Should return null for non-existent key ID")
  void shouldReturnNullForNonExistentKeyId() {
    // Act
    var keyPair = service.getKeyPairById("non-existent-key-id");

    // Assert
    assertThat(keyPair).isNull();
  }

  @Test
  @DisplayName("Should get JWK set with all active keys")
  void shouldGetJwkSet() {
    // Act
    var jwkSet = service.getJwkSet();

    // Assert
    assertThat(jwkSet).isNotNull();
    assertThat(jwkSet.getKeys()).isNotEmpty();
    assertThat(jwkSet.getKeys()).hasSize(1);
  }

  @Test
  @DisplayName("Should include multiple keys in JWK set after rotation")
  void shouldIncludeMultipleKeysInJwkSetAfterRotation() {
    // Arrange
    service.rotateKeys();

    // Act
    var jwkSet = service.getJwkSet();

    // Assert
    assertThat(jwkSet).isNotNull();
    assertThat(jwkSet.getKeys()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("Should generate RSA keys with correct algorithm")
  void shouldGenerateRsaKeysWithCorrectAlgorithm() {
    // Act
    var keyPair = service.getCurrentKeyPair();

    // Assert
    assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
    assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
  }
}
