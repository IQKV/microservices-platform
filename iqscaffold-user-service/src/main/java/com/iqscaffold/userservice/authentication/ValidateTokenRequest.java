package com.iqscaffold.userservice.authentication;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
    @NotBlank
    String token
) {

}
