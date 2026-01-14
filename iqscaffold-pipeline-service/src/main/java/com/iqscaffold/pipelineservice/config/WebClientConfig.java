package com.iqscaffold.pipelineservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used for inter-service communication.
 */
@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient(final WebClient.Builder builder) {
    return builder
        .build();
  }
}
