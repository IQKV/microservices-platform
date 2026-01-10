package com.iqscaffold.billingservice.payment;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entity representing a customer in a payment gateway system.
 * This abstraction allows support for multiple payment providers (Stripe, PayPal, etc.)
 * without coupling the domain model to a specific gateway.
 */
@Entity
@Table(name = "payment_gateway_customer")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.payment.PaymentGatewayCustomer")
public class PaymentGatewayCustomer {

  @Id
  private UUID id;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "name")
  private String name;

  /**
   * The customer ID in the payment gateway system (e.g., Stripe customer ID, PayPal payer ID).
   */
  @Column(name = "gateway_customer_id", nullable = false)
  private String gatewayCustomerId;

  /**
   * The connected account ID in the payment gateway (for marketplace/platform scenarios).
   * Null for platform-level customers.
   */
  @Column(name = "gateway_account_id")
  private String gatewayAccountId;

  /**
   * The payment gateway provider type.
   */
  @Column(name = "gateway_provider", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private PaymentGatewayProvider gatewayProvider;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public PaymentGatewayCustomer() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGatewayCustomerId() {
    return gatewayCustomerId;
  }

  public void setGatewayCustomerId(String gatewayCustomerId) {
    this.gatewayCustomerId = gatewayCustomerId;
  }

  public String getGatewayAccountId() {
    return gatewayAccountId;
  }

  public void setGatewayAccountId(String gatewayAccountId) {
    this.gatewayAccountId = gatewayAccountId;
  }

  public PaymentGatewayProvider getGatewayProvider() {
    return gatewayProvider;
  }

  public void setGatewayProvider(PaymentGatewayProvider gatewayProvider) {
    this.gatewayProvider = gatewayProvider;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
