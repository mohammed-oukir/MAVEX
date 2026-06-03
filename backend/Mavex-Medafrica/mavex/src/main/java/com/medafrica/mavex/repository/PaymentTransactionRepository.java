package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.payment.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /** Retrouve une transaction par la référence gateway (ex: PayPal Order ID) */
    Optional<PaymentTransaction> findByGatewayRef(String gatewayRef);

    /** Toutes les transactions d'un order */
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
