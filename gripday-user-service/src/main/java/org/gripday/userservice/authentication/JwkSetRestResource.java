package org.gripday.userservice.authentication;

import java.util.Map;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWK Set endpoint for public key distribution.
 * Allows downstream services to dynamically fetch public keys for JWT validation.
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name = "JWK Set", description = "JSON Web Key Set for JWT validation")
public class JwkSetRestResource {

  private final JWKSource<SecurityContext> jwkSource;

  public JwkSetRestResource(final JWKSource<SecurityContext> jwkSource) {
    this.jwkSource = jwkSource;
  }

  @Operation(
      summary = "Get JWK Set",
      description = "Returns the JSON Web Key Set containing public keys for JWT validation. " +
                    "Downstream services use this endpoint to dynamically fetch public keys."
  )
  @GetMapping("/jwks.json")
  public Map<String, Object> jwkSet() {
    try {
      var jwkSelector = new com.nimbusds.jose.jwk.JWKSelector(new com.nimbusds.jose.jwk.JWKMatcher.Builder().build());
      var jwks = jwkSource.get(jwkSelector, null);
      return new com.nimbusds.jose.jwk.JWKSet(jwks).toJSONObject();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to retrieve JWK Set", e);
    }
  }
}
