package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>,
        JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByGatewayRef(String gatewayRef);

    Optional<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByOrder_IdAndStatus(Long orderId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.status = 'SUCCESS'")
    BigDecimal sumSuccessAmount();
}
