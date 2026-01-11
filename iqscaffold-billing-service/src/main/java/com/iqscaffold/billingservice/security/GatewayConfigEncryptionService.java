package com.iqscaffold.billingservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting and decrypting payment gateway configuration data.
 * <p>
 * Uses AES-256-GCM encryption for securing sensitive gateway credentials
 * (API keys, secrets, tokens) before storing in the database.
 * </p>
 * <p>
 * The encryption is performed using:
 * <ul>
 *   <li>AES-256 algorithm in GCM mode (Galois/Counter Mode)</li>
 *   <li>128-bit authentication tag for integrity verification</li>
 *   <li>Unique IV (Initialization Vector) per encryption operation</li>
 *   <li>Master encryption key from environment/configuration</li>
 *   <li>Tenant-specific salt for key derivation</li>
 * </ul>
 * </p>
 */
@Service
public class GatewayConfigEncryptionService {

  private static final Logger logger = LoggerFactory.getLogger(GatewayConfigEncryptionService.class);

  private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128; // bits
  private static final int GCM_IV_LENGTH = 12; // bytes
  private static final String KEY_ALGORITHM = "AES";

  private final IqScaffoldProperties properties;
  private final SecureRandom secureRandom;

  public GatewayConfigEncryptionService(final IqScaffoldProperties properties) {
    this.properties = properties;
    this.secureRandom = new SecureRandom();
  }

  /**
   * Encrypts the gateway configuration data.
   * <p>
   * The output format is: Base64(IV + encrypted_data)
   * This allows the IV to be stored alongside the encrypted data.
   * </p>
   *
   * @param plaintext The plaintext configuration data (typically JSON)
   * @param tenantId  The tenant ID for key derivation
   * @return Base64-encoded encrypted data with IV
   * @throws RuntimeException if encryption fails
   */
  public String encrypt(String plaintext, String tenantId) {
    if (plaintext == null || plaintext.isEmpty()) {
      throw new IllegalArgumentException("Plaintext cannot be null or empty");
    }
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant ID cannot be null or empty");
    }

    try {
      // Generate unique IV for this encryption operation
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      // Derive encryption key from master key and tenant ID
      SecretKey key = deriveKey(tenantId);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

      // Encrypt the data
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Combine IV and encrypted data
      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

      // Return Base64-encoded result
      return Base64.getEncoder().encodeToString(combined);

    } catch (final Exception e) {
      logger.error("Failed to encrypt gateway configuration for tenant {}: {}", tenantId, e.getMessage(), e);
      throw new RuntimeException("Encryption failed", e);
    }
  }

  /**
   * Decrypts the gateway configuration data.
   * <p>
   * Expects input in the format: Base64(IV + encrypted_data)
   * </p>
   *
   * @param ciphertext Base64-encoded encrypted data with IV
   * @param tenantId   The tenant ID for key derivation
   * @return Decrypted plaintext configuration data
   * @throws RuntimeException if decryption fails
   */
  public String decrypt(String ciphertext, String tenantId) {
    if (ciphertext == null || ciphertext.isEmpty()) {
      throw new IllegalArgumentException("Ciphertext cannot be null or empty");
    }
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant ID cannot be null or empty");
    }

    try {
      // Decode Base64
      byte[] combined = Base64.getDecoder().decode(ciphertext);

      // Validate minimum length (IV + at least 1 byte of encrypted data + auth tag)
      if (combined.length < GCM_IV_LENGTH + 1) {
        throw new IllegalArgumentException("Invalid encrypted data length");
      }

      // Extract IV and encrypted data
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

      byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
      System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

      // Derive encryption key
      SecretKey key = deriveKey(tenantId);

      // Initialize cipher for decryption
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

      // Decrypt the data
      byte[] decrypted = cipher.doFinal(encrypted);

      return new String(decrypted, StandardCharsets.UTF_8);

    } catch (final Exception e) {
      logger.error("Failed to decrypt gateway configuration for tenant {}: {}", tenantId, e.getMessage(), e);
      throw new RuntimeException("Decryption failed", e);
    }
  }

  /**
   * Derives an encryption key from the master key and tenant ID.
   * <p>
   * This provides tenant-specific key derivation while using a single master key.
   * Uses SHA-256 for key derivation.
   * </p>
   *
   * @param tenantId The tenant ID
   * @return Derived AES secret key
   */
  private SecretKey deriveKey(String tenantId) throws Exception {
    String masterKey = properties.billing().security().encryption().masterKey();
    
    if (masterKey == null || masterKey.isEmpty()) {
      throw new IllegalStateException("Master encryption key not configured");
    }

    // Combine master key with tenant ID for tenant-specific derivation
    String keyMaterial = masterKey + ":" + tenantId;

    // Use SHA-256 to derive a 256-bit key
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] keyBytes = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));

    return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
  }

  /**
   * Masks sensitive data for display purposes.
   * Shows only the last 4 characters.
   *
   * @param sensitive The sensitive string to mask
   * @return Masked string (e.g., "****1234")
   */
  public String maskSensitiveData(String sensitive) {
    if (sensitive == null || sensitive.isEmpty()) {
      return "****";
    }
    if (sensitive.length() <= 4) {
      return "****";
    }
    String lastFour = sensitive.substring(sensitive.length() - 4);
    return "****" + lastFour;
  }

  /**
   * Extracts the last 4 characters of sensitive data for identification.
   *
   * @param sensitive The sensitive string
   * @return Last 4 characters or empty string
   */
  public String getLastFourChars(String sensitive) {
    if (sensitive == null || sensitive.isEmpty()) {
      return "";
    }
    if (sensitive.length() <= 4) {
      return sensitive;
    }
    return sensitive.substring(sensitive.length() - 4);
  }
}
