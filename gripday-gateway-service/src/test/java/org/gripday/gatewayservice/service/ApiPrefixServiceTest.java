package org.gripday.gatewayservice.service;

import org.gripday.gatewayservice.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiPrefixService Tests")
class ApiPrefixServiceTest {

  @Mock
  private GatewayProperties gatewayProperties;

  @Mock
  private GatewayProperties.Routing routing;

  @Mock
  private GatewayProperties.Routing.ApiPrefix apiPrefix;

  private ApiPrefixService apiPrefixService;

  @BeforeEach
  void setUp() {
    when(gatewayProperties.routing()).thenReturn(routing);
    when(routing.apiPrefix()).thenReturn(apiPrefix);
    apiPrefixService = new ApiPrefixService(gatewayProperties);
  }

  @Test
  @DisplayName("Should return configured prefix")
  void shouldReturnConfiguredPrefix() {
    when(apiPrefix.prefix()).thenReturn("/api");

    var result = apiPrefixService.getPrefix();

    assertThat(result).isEqualTo("/api");
  }

  @Test
  @DisplayName("Should return strip count")
  void shouldReturnStripCount() {
    when(apiPrefix.stripCount()).thenReturn(1);

    var result = apiPrefixService.getStripCount();

    assertThat(result).isEqualTo(1);
  }

  @Test
  @DisplayName("Should return enabled status")
  void shouldReturnEnabledStatus() {
    when(apiPrefix.enabled()).thenReturn(true);

    var result = apiPrefixService.isEnabled();

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should build full path with prefix")
  void shouldBuildFullPathWithPrefix() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.prefix()).thenReturn("/api");

    var result = apiPrefixService.buildFullPath("/v1/auth/login");

    assertThat(result).isEqualTo("/api/v1/auth/login");
  }

  @Test
  @DisplayName("Should return service path when prefix is empty")
  void shouldReturnServicePathWhenPrefixIsEmpty() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.prefix()).thenReturn("");

    var result = apiPrefixService.buildFullPath("/v1/auth/login");

    assertThat(result).isEqualTo("/v1/auth/login");
  }

  @Test
  @DisplayName("Should return service path when disabled")
  void shouldReturnServicePathWhenDisabled() {
    when(apiPrefix.enabled()).thenReturn(false);

    var result = apiPrefixService.buildFullPath("/v1/auth/login");

    assertThat(result).isEqualTo("/v1/auth/login");
  }

  @Test
  @DisplayName("Should strip prefix from path")
  void shouldStripPrefixFromPath() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.stripCount()).thenReturn(1);
    when(apiPrefix.prefix()).thenReturn("/api");

    var result = apiPrefixService.stripPrefix("/api/v1/auth/login");

    assertThat(result).isEqualTo("/v1/auth/login");
  }

  @Test
  @DisplayName("Should not strip when strip count is zero")
  void shouldNotStripWhenStripCountIsZero() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.stripCount()).thenReturn(0);
    when(apiPrefix.prefix()).thenReturn("/api");

    var result = apiPrefixService.stripPrefix("/api/v1/auth/login");

    assertThat(result).isEqualTo("/api/v1/auth/login");
  }

  @Test
  @DisplayName("Should check if path has prefix")
  void shouldCheckIfPathHasPrefix() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.prefix()).thenReturn("/api");

    var result = apiPrefixService.hasPrefix("/api/v1/auth/login");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should return false when path does not have prefix")
  void shouldReturnFalseWhenPathDoesNotHavePrefix() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.prefix()).thenReturn("/api");

    var result = apiPrefixService.hasPrefix("/v1/auth/login");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should normalize path with leading slash")
  void shouldNormalizePathWithLeadingSlash() {
    var result = apiPrefixService.normalizePath("v1/auth/login");

    assertThat(result).isEqualTo("/v1/auth/login");
  }

  @Test
  @DisplayName("Should keep normalized path unchanged")
  void shouldKeepNormalizedPathUnchanged() {
    var result = apiPrefixService.normalizePath("/v1/auth/login");

    assertThat(result).isEqualTo("/v1/auth/login");
  }

  @Test
  @DisplayName("Should return root for empty path")
  void shouldReturnRootForEmptyPath() {
    var result = apiPrefixService.normalizePath("");

    assertThat(result).isEqualTo("/");
  }

  @Test
  @DisplayName("Should return configuration details")
  void shouldReturnConfigurationDetails() {
    when(apiPrefix.enabled()).thenReturn(true);
    when(apiPrefix.prefix()).thenReturn("/api");
    when(apiPrefix.stripCount()).thenReturn(1);

    var result = apiPrefixService.getConfigurationDetails();

    assertThat(result).isEqualTo("ApiPrefix[enabled=true, prefix='/api', stripCount=1]");
  }
}
