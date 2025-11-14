package org.gripday.userservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
    @NotBlank
    String token
) {

}
