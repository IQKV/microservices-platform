package com.iqscaffold.userservice.config;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that require embedded Redis.
 * Extend this class to automatically configure and start an embedded Redis server.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {
}
