package org.gripday.userservice.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  void setUp() {
    provider = new SchemaPerTenantConnectionProvider(dataSource);
  }

  @Test
  @DisplayName("Should get any connection from data source")
  void shouldGetAnyConnection() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    var result = provider.getAnyConnection();
    assertThat(result).isNotNull();
    verify(dataSource).getConnection();
  }

  @Test
  @DisplayName("Should release any connection")
  void shouldReleaseAnyConnection() throws SQLException {
    provider.releaseAnyConnection(connection);
    verify(connection).close();
  }

  @Test
  @DisplayName("Should get connection with tenant schema for PostgreSQL")
  void shouldGetConnectionWithTenantSchemaForPostgreSQL() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    var result = provider.getConnection("tenant_123");
    assertThat(result).isNotNull();
    verify(connection).setSchema("tenant_123");
  }

  @Test
  @DisplayName("Should get connection with default schema when tenant is null")
  void shouldGetConnectionWithDefaultSchemaWhenTenantIsNull() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    var result = provider.getConnection(null);
    assertThat(result).isNotNull();
    verify(connection).setSchema("public");
  }

  @Test
  @DisplayName("Should get connection with PUBLIC schema for H2")
  void shouldGetConnectionWithPublicSchemaForH2() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("H2");
    var result = provider.getConnection("public");
    assertThat(result).isNotNull();
    verify(connection).setSchema("PUBLIC");
  }

  @Test
  @DisplayName("Should create schema for H2 when tenant is not PUBLIC")
  void shouldCreateSchemaForH2WhenTenantIsNotPublic() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("H2");
    when(connection.createStatement()).thenReturn(statement);
    
    var result = provider.getConnection("tenant_123");
    
    assertThat(result).isNotNull();
    verify(statement).execute("CREATE SCHEMA IF NOT EXISTS tenant_123");
    verify(statement).close();
    verify(connection).setSchema("tenant_123");
  }

  @Test
  @DisplayName("Should release connection and reset schema to public")
  void shouldReleaseConnectionAndResetSchemaToPublic() throws SQLException {
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    provider.releaseConnection("tenant_123", connection);
    verify(connection).setSchema("public");
    verify(connection).close();
  }

  @Test
  @DisplayName("Should release connection and reset schema to PUBLIC for H2")
  void shouldReleaseConnectionAndResetSchemaToPublicForH2() throws SQLException {
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("H2");
    provider.releaseConnection("tenant_123", connection);
    verify(connection).setSchema("PUBLIC");
    verify(connection).close();
  }

  @Test
  @DisplayName("Should return false for isUnwrappableAs")
  void shouldReturnFalseForIsUnwrappableAs() {
    var result = provider.isUnwrappableAs(DataSource.class);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should return null for unwrap")
  void shouldReturnNullForUnwrap() {
    var result = provider.unwrap(DataSource.class);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return false for supportsAggressiveRelease")
  void shouldReturnFalseForSupportsAggressiveRelease() {
    var result = provider.supportsAggressiveRelease();
    assertThat(result).isFalse();
  }
}
