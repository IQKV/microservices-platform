package org.gripday.userservice.config;

import java.time.Duration;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.gripday.userservice.authentication.JwtKeyManagementService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * JWT configuration for token generation and validation. 
 * Uses RSA256 algorithm with key rotation support.
 */
@Configuration
@ConfigurationProperties(prefix = "gripday.auth.jwt")
public class JwtConfiguration {

  private Duration accessTokenExpiry = Duration.ofMinutes(15);
  private Duration refreshTokenExpiry = Duration.ofDays(7);
  private String issuer = "gripday-user-service";

  private final JwtKeyManagementService keyManagementService;

  public JwtConfiguration(final JwtKeyManagementService keyManagementService) {
    this.keyManagementService = keyManagementService;
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    return (jwkSelector, context) -> jwkSelector.select(keyManagementService.getJwkSet());
  }

  @Bean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    // Use JWK Set for validation (supports multiple keys during rotation)
    return NimbusJwtDecoder.withJwkSetUri("http://localhost:8080/.well-known/jwks.json").build();
  }

  // Getters and setters for configuration properties
  public Duration getAccessTokenExpiry() {
    return accessTokenExpiry;
  }

  public void setAccessTokenExpiry(Duration accessTokenExpiry) {
    this.accessTokenExpiry = accessTokenExpiry;
  }

  public Duration getRefreshTokenExpiry() {
    return refreshTokenExpiry;
  }

  public void setRefreshTokenExpiry(Duration refreshTokenExpiry) {
    this.refreshTokenExpiry = refreshTokenExpiry;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }
}