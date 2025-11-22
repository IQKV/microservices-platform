package org.gripday.userservice.usermanagement;

import jakarta.validation.Valid;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user preference operations allowing users to manage their own preferences.
 */
@RestController
@RequestMapping("/api/v1/users/me/preferences")
@Tag(name = "User Preferences", description = "User preference management operations for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class UserPreferenceRestResource {

  private final UserPreferenceService preferenceService;

  public UserPreferenceRestResource(final UserPreferenceService preferenceService) {
    this.preferenceService = preferenceService;
  }

  @Operation(
      summary = "Get my preferences",
      description = """
          Retrieve preferences for the authenticated user. If preferences don't exist, 
          default preferences are automatically created and returned.
          
          ## Features
          - Automatic creation of default preferences
          - Localization settings (locale, timezone, currency)
          - Display preferences (theme, date/time formats)
          - Profile information (photo, phone, bio)
          - Notification preferences (email, SMS, push)
          - Two-factor authentication settings
          - Custom settings for application-specific data
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @GetMapping
  @Timed(value = "user.preference.endpoint", extraTags = {"endpoint", "get"})
  public ResponseEntity<UserPreferenceDto> getMyPreferences(
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var preferences = preferenceService.getMyPreferences(currentUser);
    return ResponseEntity.ok(preferences);
  }

  @Operation(
      summary = "Update my preferences",
      description = """
          Update preferences for the authenticated user. All fields are optional - 
          only provided fields will be updated.
          
          ## Updatable Settings
          - **Localization**: locale, timezone, currency, date/time formats
          - **Display**: theme (light/dark/auto)
          - **Profile**: photo URL, phone number, bio
          - **Notifications**: email, SMS, push notification preferences
          - **Security**: two-factor authentication settings
          - **Custom**: application-specific settings (JSON string)
          
          ## Validation
          - Theme must be 'light', 'dark', or 'auto'
          - Two-factor method must be 'sms', 'email', or 'app'
          - URLs and text fields have maximum length constraints
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or validation errors"),
      @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  @PatchMapping
  @Timed(value = "user.preference.endpoint", extraTags = {"endpoint", "update"})
  public ResponseEntity<UserPreferenceDto> updateMyPreferences(
      @Parameter(description = "Preference update request", required = true)
      @Valid @RequestBody UpdateUserPreferenceRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    var updatedPreferences = preferenceService.updateMyPreferences(request, currentUser);
    return ResponseEntity.ok(updatedPreferences);
  }

  @Operation(
      summary = "Delete my preferences",
      description = """
          Delete preferences for the authenticated user and reset to defaults.
          
          ## Behavior
          - Removes all custom preference settings
          - Next GET request will create new default preferences
          - Cannot be undone - use with caution
          
          ## Use Cases
          - Reset all settings to defaults
          - Clear personalization data
          - Privacy/data cleanup
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Preferences deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "404", description = "Preferences not found")
  })
  @DeleteMapping
  @Timed(value = "user.preference.endpoint", extraTags = {"endpoint", "delete"})
  public ResponseEntity<Void> deleteMyPreferences(
      @Parameter(hidden = true) @AuthenticationPrincipal UserContext currentUser) {

    preferenceService.deleteMyPreferences(currentUser);
    return ResponseEntity.noContent().build();
  }
}
