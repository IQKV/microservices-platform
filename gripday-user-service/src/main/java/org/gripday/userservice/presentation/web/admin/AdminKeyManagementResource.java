package org.gripday.userservice.presentation.web.admin;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.userservice.domain.service.JwtKeyManagementService;
import org.gripday.userservice.domain.service.TokenCleanupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints for key management and token cleanup operations.
 * Restricted to SUPER_ADMIN role only.
 */
@RestController
@RequestMapping("/api/v1/admin/keys")
@Tag(name = "Admin - Key Management", description = "Administrative endpoints for JWT key rotation and token cleanup")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminKeyManagementResource {

  private final JwtKeyManagementService keyManagementService;
  private final TokenCleanupService tokenCleanupService;

  public AdminKeyManagementResource(
      final JwtKeyManagementService keyManagementService,
      final TokenCleanupService tokenCleanupService) {
    this.keyManagementService = keyManagementService;
    this.tokenCleanupService = tokenCleanupService;
  }

  @Operation(
      summary = "Rotate JWT signing keys",
      description = "Manually trigger JWT key rotation. Old keys are kept for validation during grace period."
  )
  @PostMapping("/rotate")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<Map<String, Object>> rotateKeys() {
    keyManagementService.rotateKeys();
    
    return ResponseEntity.ok(Map.of(
        "message", "JWT keys rotated successfully",
        "currentKeyId", keyManagementService.getCurrentKeyId()
    ));
  }

  @Operation(
      summary = "Trigger token cleanup",
      description = "Manually trigger cleanup of expired tokens and sessions from Redis"
  )
  @PostMapping("/cleanup")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<Map<String, String>> cleanupTokens() {
    tokenCleanupService.cleanupExpiredTokens();
    
    return ResponseEntity.ok(Map.of(
        "message", "Token cleanup completed successfully"
    ));
  }
}
