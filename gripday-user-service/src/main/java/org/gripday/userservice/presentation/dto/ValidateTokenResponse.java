package org.gripday.userservice.presentation.dto;

import java.time.Instant;

public record ValidateTokenResponse(
    boolean active,
    String tokenId,
    String tokenType,
    Instant issuedAt,
    Instant expiresAt,
    UserContext user
) {

}
