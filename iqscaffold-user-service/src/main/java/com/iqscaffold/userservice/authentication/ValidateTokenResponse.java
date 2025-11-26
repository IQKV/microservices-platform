package com.iqscaffold.userservice.authentication;

import java.time.Instant;

import com.iqscaffold.userservice.usermanagement.UserContext;

public record ValidateTokenResponse(
    boolean active,
    String tokenId,
    String tokenType,
    Instant issuedAt,
    Instant expiresAt,
    UserContext user
) {

}
