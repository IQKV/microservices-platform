package org.gripday.bookstore.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JWT Configuration for Bookstore Service (Downstream Microservice).
 *
 * <p>This service validates JWT tokens using RSA256 asymmetric encryption by fetching
 * public keys from the User Service's JWK endpoint. This follows the Gripday platform's
 * centralized authentication architecture.
 *
 * <p><b>Architecture Pattern:</b>
 * <ul>
 *   <li>User Service: Generates JWT tokens with RSA256 private key</li>
 *   <li>Gateway Service: Validates tokens using RSA256 public key from JWK endpoint</li>
 *   <li>Downstream Services (this): Validate tokens using RSA256 public key from JWK endpoint</li>
 * </ul>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Automatic public key fetching from JWK endpoint</li>
 *   <li>Zero-configuration key distribution</li>
 *   <li>Automatic key rotation support (90-day rotation with 7-day grace period)</li>
 *   <li>No shared secrets required</li>
 *   <li>Spring Security caches keys (5 minutes default)</li>
 * </ul>
 *
 * <p><b>Configuration:</b>
 * <pre>
 * spring:
 *   security:
 *     oauth2:
 *       resourceserver:
 *         jwt:
 *           issuer-uri: http://user-service:8080
 *           jwk-set-uri: http://user-service:8080/.well-known/jwks.json
 * </pre>
 *
 * @see <a href="docs/auth/authentication-architecture.md">Authentication Architecture</a>
 */
@Configuration
@Profile("!test")
public class JwtConfiguration {

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  /**
   * Creates a JWT decoder that validates tokens using RSA256 public keys from JWK endpoint.
   *
   * <p>The decoder automatically:
   * <ul>
   *   <li>Fetches public keys from User Service's JWK endpoint</li>
   *   <li>Caches keys for 5 minutes (Spring Security default)</li>
   *   <li>Refreshes keys on rotation (supports multiple active keys during grace period)</li>
   *   <li>Validates token signature using RSA256 algorithm</li>
   *   <li>Validates issuer, expiry, and other standard JWT claims</li>
   * </ul>
   *
   * @return JwtDecoder configured for RSA256 validation via JWK endpoint
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }
}