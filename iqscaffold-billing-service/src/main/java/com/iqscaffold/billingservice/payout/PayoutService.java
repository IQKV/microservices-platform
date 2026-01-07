package com.iqscaffold.billingservice.payout;

import java.math.BigDecimal;
import java.time.Instant;

import com.stripe.model.Payout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayoutService {
  private static final Logger logger = LoggerFactory.getLogger(PayoutService.class);
  private final PayoutRepository payoutRepository;

  public PayoutService(final PayoutRepository payoutRepository) {
    this.payoutRepository = payoutRepository;
  }

  @Transactional
  public void processPayout(Payout stripePayout) {
    com.iqscaffold.billingservice.payout.Payout payoutEntity = new com.iqscaffold.billingservice.payout.Payout();

    payoutEntity.setId(stripePayout.getId());
    payoutEntity.setAmount(BigDecimal.valueOf(stripePayout.getAmount()).divide(BigDecimal.valueOf(100))); // Convert from minor
    payoutEntity.setCurrency(stripePayout.getCurrency());
    payoutEntity.setStatus(stripePayout.getStatus());
    payoutEntity.setArrivalDate(Instant.ofEpochSecond(stripePayout.getArrivalDate()));

    // In a real Connect scenario, the payout webhook usually comes from the connected account
    // or contains metadata. For now, we store what we have.

    payoutRepository.save(payoutEntity);
    logger.info("Processed payout: {} | Status: {}", stripePayout.getId(), stripePayout.getStatus());
  }
}
