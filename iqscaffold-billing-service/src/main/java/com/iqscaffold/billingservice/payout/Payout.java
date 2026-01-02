package com.iqscaffold.billingservice.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payout")
@Getter
@Setter
@NoArgsConstructor
public class Payout {
  @Id
  private String id; // Stripe Payout ID

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "arrival_date")
  private Instant arrivalDate;

  @Column(name = "status")
  private String status;

  @Column(name = "merchant_account_id")
  private String merchantAccountId;
  
  // Note: Tenancy is tricky for payouts if they are platform-wide, 
  // but if they are for a connected account, we can trace back to tenant via MerchantStripeConfig
}
