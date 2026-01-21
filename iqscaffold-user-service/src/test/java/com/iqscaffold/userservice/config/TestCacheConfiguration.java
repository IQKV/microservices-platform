package com.iqscaffold.userservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("test")
public class TestCacheConfiguration {

  @Bean
  @Primary
  public CacheManager testCacheManager() {
    return new ConcurrentMapCacheManager(
        "users",
        "authorities",
        "tenants",
        "jwt-blacklist",
        "rate-limits",
        "account-lockouts",
        "sessions"
    );
  }
}