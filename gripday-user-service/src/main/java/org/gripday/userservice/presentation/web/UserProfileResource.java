package org.gripday.userservice.presentation.web;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.userservice.domain.service.JwtService;
import org.gripday.userservice.presentation.dto.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for current user profile operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile", description = "Current authenticated user profile operations")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileResource {

  private final JwtService jwtService;

  public UserProfileResource(final JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get current authenticated user",
      description = "Return the user context derived from the bearer JWT used to authenticate the request.",
      tags = {"User Profile"}
  )
  @Timed(value = "auth.endpoint", extraTags = {"endpoint", "me"})
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "User context returned",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = UserContext.class),
              examples = @ExampleObject(
                  name = "User Context",
                  summary = "Authenticated user information",
                  value = """
                      {
                        "userId": 1,
                        "username": "john.doe",
                        "email": "john.doe@example.com",
                        "roles": ["USER"],
                        "permissions": [],
                        "firstName": "John",
                        "lastName": "Doe",
                        "tenantId": "tenant-123",
                        "customClaims": {}
                      }
                      """
              )
          )
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized", ref = "#/components/responses/Unauthorized")
  })
  public ResponseEntity<UserContext> getCurrentUser(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken token) {
      var user = jwtService.extractUserContext(token.getToken());
      return ResponseEntity.ok(user);
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
