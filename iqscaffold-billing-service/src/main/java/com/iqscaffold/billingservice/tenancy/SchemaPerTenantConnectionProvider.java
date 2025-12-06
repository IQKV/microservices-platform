package com.iqscaffold.billingservice.tenancy;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

/**
 * Hibernate multi-tenant connection provider for schema-per-tenant strategy.
 * Manages database connections and sets the appropriate schema for each tenant.
 */
public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider {

  private final DataSource dataSource;

  public SchemaPerTenantConnectionProvider(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Get a connection without tenant context (uses default schema).
   *
   * @return a database connection
   * @throws SQLException if connection cannot be obtained
   */
  @Override
  public Connection getAnyConnection() throws SQLException {
    return dataSource.getConnection();
  }

  /**
   * Release a connection obtained via getAnyConnection().
   *
   * @param connection the connection to release
   * @throws SQLException if connection cannot be closed
   */
  @Override
  public void releaseAnyConnection(final Connection connection) throws SQLException {
    connection.close();
  }

  /**
   * Get a connection for a specific tenant and set the schema.
   * For H2 (test database), automatically creates the schema if it doesn't exist.
   *
   * @param tenantIdentifier the tenant schema name
   * @return a database connection with schema set
   * @throws SQLException if connection cannot be obtained or schema cannot be set
   */
  @Override
  public Connection getConnection(final Object tenantIdentifier) throws SQLException {
    var connection = getAnyConnection();
    var defaultSchema = isH2(connection) ? "PUBLIC" : "public";
    var schema = tenantIdentifier != null ? String.valueOf(tenantIdentifier) : defaultSchema;
    
    // H2 uses uppercase PUBLIC schema by default
    if (isH2(connection) && "public".equalsIgnoreCase(schema)) {
      schema = "PUBLIC";
    }
    
    // Auto-create schema for H2 (test database only)
    if (isH2(connection) && !"PUBLIC".equalsIgnoreCase(schema)) {
      try (var stmt = connection.createStatement()) {
        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
      }
    }
    
    connection.setSchema(schema);
    return connection;
  }

  /**
   * Release a tenant-specific connection and reset schema to default.
   *
   * @param tenantIdentifier the tenant schema name
   * @param connection       the connection to release
   * @throws SQLException if connection cannot be closed or schema cannot be reset
   */
  @Override
  public void releaseConnection(final Object tenantIdentifier, final Connection connection) throws SQLException {
    try {
      connection.setSchema(isH2(connection) ? "PUBLIC" : "public");
    } finally {
      connection.close();
    }
  }

  @Override
  public boolean isUnwrappableAs(final Class unwrapType) {
    return false;
  }

  @Override
  public <T> T unwrap(final Class<T> unwrapType) {
    return null;
  }

  @Override
  public boolean supportsAggressiveRelease() {
    return false;
  }

  /**
   * Check if the connection is to an H2 database (used for testing).
   *
   * @param connection the database connection
   * @return true if H2, false otherwise
   * @throws SQLException if database metadata cannot be accessed
   */
  private boolean isH2(final Connection connection) throws SQLException {
    var name = connection.getMetaData().getDatabaseProductName();
    return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("h2");
  }
}
