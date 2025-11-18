package org.gripday.userservice.authentication;

import java.time.Instant;

import org.gripday.userservice.usermanagement.UserContext;

public record ValidateTokenResponse(
    boolean active,
    String tokenId,
    String tokenType,
    Instant issuedAt,
    Instant expiresAt,
    UserContext user
) {

}
