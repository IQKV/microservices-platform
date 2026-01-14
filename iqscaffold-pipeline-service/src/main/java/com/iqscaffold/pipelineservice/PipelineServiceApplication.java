package com.iqscaffold.pipelineservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.iqscaffold.pipelineservice.config")
public class PipelineServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PipelineServiceApplication.class, args);
  }
}
