package com.iqscaffold.leadservice.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configuration for WebClient used for inter-service communication.
 * <p>
 * Configures HTTP client with:
 * <ul>
 *   <li>Connection timeout</li>
 *   <li>Read/write timeouts</li>
 *   <li>Response size limits</li>
 *   <li>Base URL for contact service</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

  @Value("${iqscaffold.contact-service-url:http://contact-service:8080}")
  private String contactServiceUrl;

  @Value("${iqscaffold.pipeline-service-url:http://lead-service:8080}")
  private String pipelineServiceUrl;

  /**
   * Creates a WebClient bean for contact service communication.
   *
   * @return Configured WebClient instance
   */
  @Bean
  public WebClient contactServiceWebClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .responseTimeout(Duration.ofSeconds(5))
        .doOnConnected(conn ->
            conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

    return WebClient.builder()
        .baseUrl(contactServiceUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(16 * 1024 * 1024)) // 16MB
        .build();
  }

  /**
   * Creates a WebClient bean for pipeline service communication.
   *
   * @return Configured WebClient instance
   */
  @Bean
  public WebClient pipelineServiceWebClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .responseTimeout(Duration.ofSeconds(5))
        .doOnConnected(conn ->
            conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

    return WebClient.builder()
        .baseUrl(pipelineServiceUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(16 * 1024 * 1024)) // 16MB
        .build();
  }
}
