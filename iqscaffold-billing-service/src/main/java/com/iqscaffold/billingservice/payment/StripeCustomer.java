package com.iqscaffold.billingservice.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stripe_customer")
@Getter
@Setter
@NoArgsConstructor
public class StripeCustomer {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "name")
  private String name;

  @Column(name = "stripe_customer_id", nullable = false)
  private String stripeCustomerId;

  @Column(name = "stripe_account_id")
  private String stripeAccountId; // If customer belongs to a connected account? PHP implies this uniqueness.

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

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
