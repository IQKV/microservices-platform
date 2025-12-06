package com.iqscaffold.billingservice.usage;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * UsageRecord entity for tracking resource consumption.
 */
@Entity
@Table(name = "usage_records")
public class UsageRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Entity implementation will be added in subsequent tasks
}
