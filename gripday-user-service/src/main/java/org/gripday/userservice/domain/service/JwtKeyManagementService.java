package org.gripday.userservice.domain.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages RSA key pairs for JWT signing with rotation support.
 * Implements key rotation with grace period to allow validation of tokens signed with old keys.
 */
@Service
public class JwtKeyManagementService {

  private static final Logger logger = LoggerFactory.getLogger(JwtKeyManagementService.class);
  private static final int KEY_SIZE = 2048;
  private static final long KEY_ROTATION_GRACE_PERIOD_DAYS = 7;

  private final ConcurrentHashMap<String, KeyEntry> keys = new ConcurrentHashMap<>();
  private volatile String currentKeyId;

  public JwtKeyManagementService() {
    // Generate initial key pair
    rotateKeys();
  }

  /**
   * Rotate keys by generating a new key pair.
   * Old keys are kept for validation during grace period.
   */
  public synchronized void rotateKeys() {
    try {
      var keyId = java.util.UUID.randomUUID().toString();
      var keyPair = generateKeyPair();
      var keyEntry = new KeyEntry(keyId, keyPair, Instant.now());

      keys.put(keyId, keyEntry);
      currentKeyId = keyId;

      logger.info("Generated new RSA key pair with ID: {}", keyId);

      // Clean up old keys (older than grace period)
      cleanupOldKeys();

    } catch (final Exception e) {
      logger.error("Failed to rotate keys", e);
      throw new RuntimeException("Key rotation failed", e);
    }
  }

  /**
   * Get current key pair for signing.
   */
  public KeyPair getCurrentKeyPair() {
    var keyEntry = keys.get(currentKeyId);
    if (keyEntry == null) {
      throw new IllegalStateException("No current key pair available");
    }
    return keyEntry.keyPair();
  }

  /**
   * Get current key ID.
   */
  public String getCurrentKeyId() {
    return currentKeyId;
  }

  /**
   * Get JWK Set containing all active public keys.
   */
  public JWKSet getJwkSet() {
    var jwkList = new java.util.ArrayList<com.nimbusds.jose.jwk.JWK>();

    for (final var entry : keys.values()) {
      var publicKey = (RSAPublicKey) entry.keyPair().getPublic();
      var privateKey = (RSAPrivateKey) entry.keyPair().getPrivate();

      var jwk = new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID(entry.keyId())
          .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
          .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
          .build();

      jwkList.add(jwk);
    }

    return new JWKSet(jwkList);
  }

  /**
   * Get key pair by ID for validation.
   */
  public KeyPair getKeyPairById(String keyId) {
    var keyEntry = keys.get(keyId);
    return keyEntry != null ? keyEntry.keyPair() : null;
  }

  /**
   * Remove keys older than grace period.
   */
  private void cleanupOldKeys() {
    var cutoffTime = Instant.now().minusSeconds(KEY_ROTATION_GRACE_PERIOD_DAYS * 24 * 60 * 60);

    var removedKeys = new java.util.ArrayList<String>();
    
    for (final var entry : keys.entrySet()) {
      if (!entry.getKey().equals(currentKeyId) && entry.getValue().createdAt().isBefore(cutoffTime)) {
        keys.remove(entry.getKey());
        removedKeys.add(entry.getKey());
      }
    }

    if (!removedKeys.isEmpty()) {
      logger.info("Removed {} old key(s): {}", removedKeys.size(), removedKeys);
    }
  }

  private KeyPair generateKeyPair() {
    try {
      var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(KEY_SIZE);
      return keyPairGenerator.generateKeyPair();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to generate RSA key pair", e);
    }
  }

  /**
   * Key entry with metadata.
   */
  private record KeyEntry(
      String keyId,
      KeyPair keyPair,
      Instant createdAt
  ) {
  }
}
