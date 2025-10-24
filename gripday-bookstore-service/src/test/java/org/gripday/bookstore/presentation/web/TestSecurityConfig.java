package org.gripday.bookstore.presentation.web;

import org.gripday.bookstore.domain.dto.UserContext;
import org.gripday.bookstore.infrastructure.security.UserContextExtractor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        
        return http.build();
    }
    
    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return mock(JwtDecoder.class);
    }
    
    @Bean
    @Primary
    public UserContextExtractor testUserContextExtractor() {
        return new UserContextExtractor(testJwtDecoder()) {
            @Override
            public UserContext extractFromJwt(String jwtToken) {
                // Return a mock admin user for tests
                return new UserContext(
                    1L,
                    "test-admin",
                    "test@example.com",
                    Set.of("ADMIN"),
                    Set.of("BOOK_CREATE", "BOOK_UPDATE", "BOOK_DELETE", "INVENTORY_UPDATE"),
                    "Test Department",
                    "TEST_ORG",
                    Map.of()
                );
            }
        };
    }
}