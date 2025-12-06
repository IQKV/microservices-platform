package com.iqscaffold.billingservice.paymentmethod;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PaymentMethod aggregate root entity.
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Entity implementation will be added in subsequent tasks
}
