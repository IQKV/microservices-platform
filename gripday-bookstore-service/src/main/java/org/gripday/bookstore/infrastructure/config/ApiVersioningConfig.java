package org.gripday.bookstore.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

  public static final String API_VERSION_1 = "application/vnd.gripday.bookstore.v1+json";
  public static final String API_VERSION_2 = "application/vnd.gripday.bookstore.v2+json";

  private final ApiVersionInterceptor apiVersionInterceptor;

  public ApiVersioningConfig(ApiVersionInterceptor apiVersionInterceptor) {
    this.apiVersionInterceptor = apiVersionInterceptor;
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
        .favorParameter(false)
        .favorPathExtension(false)
        .ignoreAcceptHeader(false)
        .useRegisteredExtensionsOnly(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("json", MediaType.APPLICATION_JSON)
        .mediaType("v1", MediaType.parseMediaType(API_VERSION_1))
        .mediaType("v2", MediaType.parseMediaType(API_VERSION_2));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(apiVersionInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/api-docs/**");
  }
}