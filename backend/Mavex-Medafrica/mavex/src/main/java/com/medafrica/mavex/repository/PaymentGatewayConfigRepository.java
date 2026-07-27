package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfig, Long> {

    Optional<PaymentGatewayConfig> findByActiveTrue();

    Optional<PaymentGatewayConfig> findByType(PaymentGatewayType type);

    /** Desactive toutes les configs en une seule requete — appele avant activate() */
    @Modifying
    @Query("UPDATE PaymentGatewayConfig c SET c.active = false")
    void deactivateAll();
}
