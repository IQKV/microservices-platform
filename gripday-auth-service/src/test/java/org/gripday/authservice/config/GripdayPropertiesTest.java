package org.gripday.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for GripdayProperties configuration binding.
 * Validates that configuration properties are properly loaded and bound.
 */
@SpringBootTest
@ActiveProfiles("test")
class GripdayPropertiesTest {

    @Autowired
    private GripdayProperties gripdayProperties;

    @Test
    void shouldLoadDatabaseProperties() {
        var database = gripdayProperties.database();
        
        assertThat(database).isNotNull();
        assertThat(database.host()).isEqualTo("localhost");
        assertThat(database.port()).isEqualTo(5432);
        assertThat(database.name()).isEqualTo("testdb");
        assertThat(database.username()).isEqualTo("sa");
    }

    @Test
    void shouldLoadAuthProperties() {
        var auth = gripdayProperties.auth();
        
        assertThat(auth).isNotNull();
        assertThat(auth.jwt()).isNotNull();
        assertThat(auth.jwt().accessTokenExpirationMinutes()).isEqualTo(15);
        assertThat(auth.jwt().refreshTokenExpirationDays()).isEqualTo(7);
        assertThat(auth.jwt().issuer()).isEqualTo("test-issuer");
        assertThat(auth.jwt().audience()).isEqualTo("test-audience");
    }

    @Test
    void shouldLoadCacheProperties() {
        var cache = gripdayProperties.cache();
        
        assertThat(cache).isNotNull();
        assertThat(cache.redis()).isNotNull();
        assertThat(cache.redis().host()).isEqualTo("localhost");
        assertThat(cache.redis().port()).isEqualTo(6379);
        assertThat(cache.redis().database()).isEqualTo(15);
    }

    @Test
    void shouldLoadObservabilityProperties() {
        var observability = gripdayProperties.observability();
        
        assertThat(observability).isNotNull();
        assertThat(observability.tracing()).isNotNull();
        assertThat(observability.tracing().enabled()).isFalse();
        assertThat(observability.tracing().serviceName()).isEqualTo("test-auth-service");
        assertThat(observability.metrics().enabled()).isFalse();
    }
}