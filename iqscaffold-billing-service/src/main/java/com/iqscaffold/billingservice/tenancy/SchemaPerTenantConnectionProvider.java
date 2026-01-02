package com.iqscaffold.billingservice.tenancy;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider {

  private final DataSource dataSource;

  public SchemaPerTenantConnectionProvider(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public void releaseAnyConnection(final Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public Connection getConnection(final Object tenantIdentifier) throws SQLException {
    var connection = getAnyConnection();
    var defaultSchema = isH2(connection) ? "PUBLIC" : "public";
    var schema = tenantIdentifier != null ? String.valueOf(tenantIdentifier) : defaultSchema;
    if (isH2(connection) && "public".equalsIgnoreCase(schema)) {
      schema = "PUBLIC";
    }
    if (isH2(connection) && !"PUBLIC".equalsIgnoreCase(schema)) {
      try (var stmt = connection.createStatement()) {
        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
      }
    }
    connection.setSchema(schema);
    return connection;
  }

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

  private boolean isH2(final Connection connection) throws SQLException {
    var name = connection.getMetaData().getDatabaseProductName();
    return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("h2");
  }
}
