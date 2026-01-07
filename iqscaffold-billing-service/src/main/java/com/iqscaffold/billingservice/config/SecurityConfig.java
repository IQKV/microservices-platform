package com.iqscaffold.billingservice.config;

import com.iqscaffold.billingservice.security.JwtAuthenticationFilter;
import com.iqscaffold.billingservice.security.JwtClaimNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final IqScaffoldProperties iqScaffoldProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(final IqScaffoldProperties iqScaffoldProperties, final JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.iqScaffoldProperties = iqScaffoldProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**"))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/error",
                "/api/v1/billing/webhooks/**")
            .permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(jwtDecoder())
            .jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(iqScaffoldProperties.billing().security().jwt().jwkSetUri()).build();
  }

  @Bean
  public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
    var grantedAuthoritiesConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName(JwtClaimNames.AUTHORITIES);
    grantedAuthoritiesConverter.setAuthorityPrefix(""); // usage: authorities are strict

    var jwtAuthenticationConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }
}
