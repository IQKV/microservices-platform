package com.iqscaffold.billingservice.admin;

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
@Table(name = "merchant_stripe_config")
@Getter
@Setter
@NoArgsConstructor
public class MerchantStripeConfig {
  @Id
  private UUID id;


  @Column(name = "stripe_account_id")
  private String stripeAccountId;
  
  @Column(name = "charges_enabled")
  private boolean chargesEnabled;

  @Column(name = "payouts_enabled")
  private boolean payoutsEnabled;

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
