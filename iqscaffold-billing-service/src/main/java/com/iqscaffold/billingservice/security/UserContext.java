package com.iqscaffold.billingservice.security;

import java.util.Set;

public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> authorities,
    String tenantId,
    String firstName,
    String lastName
) {
    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    public boolean isAdmin() {
        return authorities.contains("ROLE_ADMIN") || authorities.contains("ADMIN");
    }
}
