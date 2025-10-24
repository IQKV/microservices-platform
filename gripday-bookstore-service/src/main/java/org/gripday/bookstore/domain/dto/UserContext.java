package org.gripday.bookstore.domain.dto;

import java.util.Map;
import java.util.Set;

public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> roles,
    Set<String> permissions,
    String department,
    String organizationId,
    Map<String, Object> customClaims
) {}