package com.iqscaffold.billingservice.payment;

import java.util.Optional;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentGatewayCustomerRepository extends JpaRepository<PaymentGatewayCustomer, UUID> {

  /**
   * Find customer by email, gateway account, and provider.
   * This ensures uniqueness per email per connected account per provider.
   */
  @Query("""
      SELECT c FROM PaymentGatewayCustomer c
      WHERE c.email = :email
      AND (:gatewayAccountId IS NULL AND c.gatewayAccountId IS NULL 
           OR c.gatewayAccountId = :gatewayAccountId)
      AND c.gatewayProvider = :provider
      """)
  Optional<PaymentGatewayCustomer> findByEmailAndGatewayAccountIdAndProvider(
      @Param("email") String email,
      @Param("gatewayAccountId") String gatewayAccountId,
      @Param("provider") PaymentGatewayProvider provider);

  /**
   * Find customer by gateway customer ID and provider.
   */
  Optional<PaymentGatewayCustomer> findByGatewayCustomerIdAndGatewayProvider(
      String gatewayCustomerId,
      PaymentGatewayProvider gatewayProvider);
}
