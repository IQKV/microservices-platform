package org.gripday.gatewayservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtClaimNames Tests")
class JwtClaimNamesTest {

    @Test
    @DisplayName("Should have correct standard JWT claim names")
    void shouldHaveCorrectStandardJwtClaimNames() {
        assertThat(JwtClaimNames.SUBJECT).isEqualTo("sub");
        assertThat(JwtClaimNames.ISSUER).isEqualTo("iss");
        assertThat(JwtClaimNames.ISSUED_AT).isEqualTo("iat");
        assertThat(JwtClaimNames.EXPIRATION).isEqualTo("exp");
        assertThat(JwtClaimNames.JWT_ID).isEqualTo("jti");
    }

    @Test
    @DisplayName("Should have correct custom Gripday claim names")
    void shouldHaveCorrectCustomGripdayClaimNames() {
        assertThat(JwtClaimNames.TYPE).isEqualTo("type");
        assertThat(JwtClaimNames.USER_ID).isEqualTo("userId");
        assertThat(JwtClaimNames.USERNAME).isEqualTo("username");
        assertThat(JwtClaimNames.EMAIL).isEqualTo("email");
        assertThat(JwtClaimNames.ROLES).isEqualTo("roles");
        assertThat(JwtClaimNames.PERMISSIONS).isEqualTo("permissions");
        assertThat(JwtClaimNames.DEPARTMENT).isEqualTo("department");
        assertThat(JwtClaimNames.ORGANIZATION_ID).isEqualTo("organizationId");
        assertThat(JwtClaimNames.TENANT_ID).isEqualTo("tenant_id");
    }

    @Test
    @DisplayName("Should throw exception when trying to instantiate utility class")
    void shouldThrowExceptionWhenTryingToInstantiateUtilityClass() {
        assertThatThrownBy(() -> {
            var constructor = JwtClaimNames.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .cause()
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("This is a utility class and cannot be instantiated");
    }
}
