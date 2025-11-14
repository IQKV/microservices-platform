package org.gripday.userservice.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Test configuration for DataSource and JPA to ensure H2 database is properly configured for tests.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.class,
    org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class
})
public class TestDataSourceConfig {

  @Bean
  @Primary
  public DataSource dataSource() {
    var config = new HikariConfig();
    config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL");
    config.setUsername("sa");
    config.setPassword("");
    config.setDriverClassName("org.h2.Driver");
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);
    return new HikariDataSource(config);
  }

  @Bean
  @Primary
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    var em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("org.gripday.userservice.infrastructure.entity");

    var vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    var properties = new Properties();
    properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    properties.setProperty("hibernate.show_sql", "false");
    properties.setProperty("hibernate.format_sql", "false");
    em.setJpaProperties(properties);

    return em;
  }

  @Bean
  @Primary
  public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    var transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory);
    return transactionManager;
  }

  @Bean
  @Primary
  public org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory() {
    // Return a mock RedisConnectionFactory for tests with proper stubbing
    var mockFactory = org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
    var mockConnection = org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
    var mockStringCommands = org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisStringCommands.class);
    var mockKeyCommands = org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisKeyCommands.class);
    
    org.mockito.Mockito.when(mockFactory.getConnection()).thenReturn(mockConnection);
    org.mockito.Mockito.when(mockConnection.stringCommands()).thenReturn(mockStringCommands);
    org.mockito.Mockito.when(mockConnection.keyCommands()).thenReturn(mockKeyCommands);
    org.mockito.Mockito.when(mockConnection.isPipelined()).thenReturn(false);
    org.mockito.Mockito.when(mockConnection.isQueueing()).thenReturn(false);
    
    // Mock common operations to return null/empty
    org.mockito.Mockito.when(mockStringCommands.get(org.mockito.Mockito.any(byte[].class))).thenReturn(null);
    org.mockito.Mockito.when(mockKeyCommands.exists(org.mockito.Mockito.any(byte[].class))).thenReturn(false);
    
    return mockFactory;
  }

  @Bean
  public org.springframework.data.redis.core.RedisTemplate<String, String> stringRedisTemplate() {
    // Return a mock RedisTemplate<String, String> for tests
    return org.mockito.Mockito.mock(org.springframework.data.redis.core.RedisTemplate.class);
  }
}
