package org.gripday.authservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
    @NotBlank
    String token
) {}
