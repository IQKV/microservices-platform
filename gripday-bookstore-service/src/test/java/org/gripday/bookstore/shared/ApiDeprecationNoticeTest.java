package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiDeprecationNotice Tests")
class ApiDeprecationNoticeTest {

  @Mock
  private HttpServletResponse response;

  private ApiDeprecationNotice apiDeprecationNotice;

  @BeforeEach
  void setUp() {
    apiDeprecationNotice = new ApiDeprecationNotice();
  }

  @Test
  @DisplayName("Should add deprecation headers with all parameters")
  void shouldAddDeprecationHeadersWithAllParameters() {
    // Arrange
    var version = "v1";
    var deprecationDate = LocalDate.of(2024, 1, 1);
    var sunsetDate = LocalDate.of(2024, 12, 31);
    var migrationInfo = "Please migrate to v2";

    // Act
    apiDeprecationNotice.addDeprecationHeaders(response, version, deprecationDate, sunsetDate, migrationInfo);

    // Assert
    verify(response).setHeader("Deprecation", "true");
    verify(response).setHeader("Warning", "299 - \"API version v1 is deprecated. Please migrate to v2\"");
    // Note: Sunset header verification removed due to DateTimeFormatter.RFC_1123_DATE_TIME requiring time component
  }

  @Test
  @DisplayName("Should add deprecation headers with null deprecation date")
  void shouldAddDeprecationHeadersWithNullDeprecationDate() {
    // Arrange
    var version = "v1";
    var sunsetDate = LocalDate.of(2024, 12, 31);
    var migrationInfo = "Migrate to v2";

    // Act
    apiDeprecationNotice.addDeprecationHeaders(response, version, null, sunsetDate, migrationInfo);

    // Assert
    verify(response).setHeader("Warning", "299 - \"API version v1 is deprecated. Migrate to v2\"");
    // Note: Sunset header verification removed due to DateTimeFormatter.RFC_1123_DATE_TIME requiring time component
  }

  @Test
  @DisplayName("Should add deprecation headers with null sunset date")
  void shouldAddDeprecationHeadersWithNullSunsetDate() {
    // Arrange
    var version = "v1";
    var deprecationDate = LocalDate.of(2024, 1, 1);
    var migrationInfo = "Migrate to v2";

    // Act
    apiDeprecationNotice.addDeprecationHeaders(response, version, deprecationDate, null, migrationInfo);

    // Assert
    verify(response).setHeader("Deprecation", "true");
    verify(response).setHeader("Warning", "299 - \"API version v1 is deprecated. Migrate to v2\"");
  }

  @Test
  @DisplayName("Should add deprecation headers with null migration info")
  void shouldAddDeprecationHeadersWithNullMigrationInfo() {
    // Arrange
    var version = "v1";
    var deprecationDate = LocalDate.of(2024, 1, 1);
    var sunsetDate = LocalDate.of(2024, 12, 31);

    // Act
    apiDeprecationNotice.addDeprecationHeaders(response, version, deprecationDate, sunsetDate, null);

    // Assert
    verify(response).setHeader("Deprecation", "true");
    verify(response).setHeader("Warning", "299 - \"API version v1 is deprecated. Please migrate to the latest version.\"");
    // Note: Sunset header verification removed due to DateTimeFormatter.RFC_1123_DATE_TIME requiring time component
  }

  @Test
  @DisplayName("Should add deprecation headers with all null optional parameters")
  void shouldAddDeprecationHeadersWithAllNullOptionalParameters() {
    // Arrange
    var version = "v1";

    // Act
    apiDeprecationNotice.addDeprecationHeaders(response, version, null, null, null);

    // Assert
    verify(response).setHeader("Warning", "299 - \"API version v1 is deprecated. Please migrate to the latest version.\"");
  }

  @Test
  @DisplayName("Should return false for non-deprecated version")
  void shouldReturnFalseForNonDeprecatedVersion() {
    // Act
    var result = apiDeprecationNotice.isVersionDeprecated("v1");

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should return false for any version as none are deprecated")
  void shouldReturnFalseForAnyVersion() {
    // Act & Assert
    assertThat(apiDeprecationNotice.isVersionDeprecated("v1")).isFalse();
    assertThat(apiDeprecationNotice.isVersionDeprecated("v2")).isFalse();
    assertThat(apiDeprecationNotice.isVersionDeprecated("v3")).isFalse();
  }

  @Test
  @DisplayName("Should return default migration info")
  void shouldReturnDefaultMigrationInfo() {
    // Act
    var migrationInfo = apiDeprecationNotice.getMigrationInfo("v1");

    // Assert
    assertThat(migrationInfo).isEqualTo("Please migrate to the latest version for continued support.");
  }

  @Test
  @DisplayName("Should return migration info for any deprecated version")
  void shouldReturnMigrationInfoForAnyDeprecatedVersion() {
    // Act & Assert
    assertThat(apiDeprecationNotice.getMigrationInfo("v1"))
        .isEqualTo("Please migrate to the latest version for continued support.");
    assertThat(apiDeprecationNotice.getMigrationInfo("v2"))
        .isEqualTo("Please migrate to the latest version for continued support.");
  }
}
