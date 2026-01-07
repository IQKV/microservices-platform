package com.iqscaffold.billingservice.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payout")
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

  public Payout() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Instant getArrivalDate() {
    return arrivalDate;
  }

  public void setArrivalDate(Instant arrivalDate) {
    this.arrivalDate = arrivalDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMerchantAccountId() {
    return merchantAccountId;
  }

  public void setMerchantAccountId(String merchantAccountId) {
    this.merchantAccountId = merchantAccountId;
  }
}
