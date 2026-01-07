package com.iqscaffold.billingservice.tenancy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchemaPerTenantConnectionProviderTest {

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private DatabaseMetaData metaData;

  @Mock
  private Statement statement;

  private SchemaPerTenantConnectionProvider provider;

  @BeforeEach
  void setUp() throws SQLException {
    provider = new SchemaPerTenantConnectionProvider(dataSource);
    lenient().when(dataSource.getConnection()).thenReturn(connection);
    lenient().when(connection.getMetaData()).thenReturn(metaData);
  }

  @Test
  void getAnyConnection_shouldReturnConnection() throws SQLException {
    // When
    Connection result = provider.getAnyConnection();

    // Then
    assertNotNull(result);
    verify(dataSource).getConnection();
    
    // Clean up
    result.close();
  }

  @Test
  void releaseAnyConnection_shouldCloseConnection() throws SQLException {
    // When
    provider.releaseAnyConnection(connection);

    // Then
    verify(connection).close();
  }

  @Test
  void getConnection_shouldSetSchemaForPostgres() throws SQLException {
    // Given
    String tenantId = "tenant-123";
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

    // When
    Connection result = provider.getConnection(tenantId);

    // Then
    verify(connection).setSchema(tenantId);
    assertNotNull(result);
  }

  @Test
  void getConnection_shouldUsePublicSchemaWhenTenantIsNull() throws SQLException {
    // Given
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

    // When
    Connection result = provider.getConnection(null);

    // Then
    verify(connection).setSchema("public");
    assertNotNull(result);
  }

  @Test
  void getConnection_shouldCreateSchemaForH2() throws SQLException {
    // Given
    String tenantId = "tenant-456";
    when(metaData.getDatabaseProductName()).thenReturn("H2");
    when(connection.createStatement()).thenReturn(statement);

    // When
    Connection result = provider.getConnection(tenantId);

    // Then
    verify(statement).execute("CREATE SCHEMA IF NOT EXISTS " + tenantId);
    verify(statement).close();
    verify(connection).setSchema(tenantId);
    assertNotNull(result);
  }

  @Test
  void getConnection_shouldUsePublicForH2WhenTenantIsNull() throws SQLException {
    // Given
    when(metaData.getDatabaseProductName()).thenReturn("H2");

    // When
    Connection result = provider.getConnection(null);

    // Then
    verify(connection).setSchema("PUBLIC");
    assertNotNull(result);
  }

  @Test
  void getConnection_shouldConvertPublicToPublicForH2() throws SQLException {
    // Given
    when(metaData.getDatabaseProductName()).thenReturn("H2");

    // When
    Connection result = provider.getConnection("public");

    // Then
    verify(connection).setSchema("PUBLIC");
    assertNotNull(result);
  }

  @Test
  void releaseConnection_shouldResetSchemaAndCloseForPostgres() throws SQLException {
    // Given
    String tenantId = "tenant-789";
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

    // When
    provider.releaseConnection(tenantId, connection);

    // Then
    verify(connection).setSchema("public");
    verify(connection).close();
  }

  @Test
  void releaseConnection_shouldResetSchemaToPublicForH2() throws SQLException {
    // Given
    String tenantId = "tenant-h2";
    when(metaData.getDatabaseProductName()).thenReturn("H2");

    // When
    provider.releaseConnection(tenantId, connection);

    // Then
    verify(connection).setSchema("PUBLIC");
    verify(connection).close();
  }

  @Test
  void releaseConnection_shouldCloseConnectionEvenIfSchemaResetFails() throws SQLException {
    // Given
    String tenantId = "tenant-error";
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    org.mockito.Mockito.doThrow(new SQLException("Schema error")).when(connection).setSchema("public");

    // When & Then
    try {
      provider.releaseConnection(tenantId, connection);
    } catch (final SQLException e) {
      // Expected
    }

    // Connection should still be closed
    verify(connection).close();
  }

  @Test
  void isUnwrappableAs_shouldReturnFalse() {
    // When
    boolean result = provider.isUnwrappableAs(DataSource.class);

    // Then
    assertFalse(result);
  }

  @Test
  void unwrap_shouldReturnNull() {
    // When
    Object result = provider.unwrap(DataSource.class);

    // Then
    assertNull(result);
  }

  @Test
  void supportsAggressiveRelease_shouldReturnFalse() {
    // When
    boolean result = provider.supportsAggressiveRelease();

    // Then
    assertFalse(result);
  }
}
