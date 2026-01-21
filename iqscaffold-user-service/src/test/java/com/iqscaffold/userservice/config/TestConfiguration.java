package com.iqscaffold.userservice.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.iqscaffold.userservice.tenancy.SchemaNameResolver;
import com.iqscaffold.userservice.tenancy.TenantContext;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@Profile("test")
public class TestConfiguration {

  @Bean
  @Primary
  public JWKSource<SecurityContext> testJwkSource() {
    try {
      final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      final KeyPair keyPair = keyPairGenerator.generateKeyPair();

      final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
      final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

      final RSAKey rsaKey = new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID("test-key-id")
          .build();

      final JWKSet jwkSet = new JWKSet(rsaKey);
      return new ImmutableJWKSet<>(jwkSet);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create test JWK source", e);
    }
  }

  @Bean
  @Primary
  public JwtEncoder testJwtEncoder(final JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  @Primary
  public JwtDecoder testJwtDecoder(final JWKSource<SecurityContext> jwkSource) {
    try {
      final RSAKey rsaKey = (RSAKey) jwkSource.get(null, null).get(0);
      return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create test JWT decoder", e);
    }
  }

  @Bean
  @Primary
  public CurrentTenantIdentifierResolver testTenantIdentifierResolver() {
    return new CurrentTenantIdentifierResolver() {
      @Override
      public String resolveCurrentTenantIdentifier() {
        final String tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? "tenant_" + tenantId : "public";
      }

      @Override
      public boolean validateExistingCurrentSessions() {
        return true;
      }
    };
  }

  @Bean
  @Primary
  public SchemaNameResolver testSchemaNameResolver() {
    return new SchemaNameResolver("tenant_");
  }
}