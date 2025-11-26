package com.iqscaffold.userservice.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

/**
 * Test configuration that starts an embedded Redis server for integration tests.
 * This allows tests to run with a real Redis instance without requiring external infrastructure.
 */
@TestConfiguration
public class TestRedisConfiguration {

  private static final Logger log = LoggerFactory.getLogger(TestRedisConfiguration.class);

  @Value("${iqscaffold.cache.redis.port:6379}")
  private int redisPort;

  private RedisServer redisServer;

  @PostConstruct
  public void startRedis() {
    try {
      redisServer = RedisServer.builder()
          .port(redisPort)
          .setting("maxmemory 128M")
          .build();

      redisServer.start();
      log.info("Embedded Redis server started on port {}", redisPort);
    } catch (final Exception e) {
      log.error("Failed to start embedded Redis server", e);
      throw new RuntimeException("Could not start embedded Redis server", e);
    }
  }

  @PreDestroy
  public void stopRedis() {
    if (redisServer != null && redisServer.isActive()) {
      try {
        redisServer.stop();
        log.info("Embedded Redis server stopped");
      } catch (final Exception e) {
        log.warn("Error stopping embedded Redis server", e);
      }
    }
  }
}
