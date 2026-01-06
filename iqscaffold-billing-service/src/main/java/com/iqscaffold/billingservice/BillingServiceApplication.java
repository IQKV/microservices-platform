package com.iqscaffold.billingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.iqscaffold.billingservice.config")
public class BillingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BillingServiceApplication.class, args);
  }
}
