package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfig, Long> {

    /** Récupère le gateway actuellement actif */
    Optional<PaymentGatewayConfig> findByActiveTrue();

    /** Vérifie si un type de gateway existe déjà */
    boolean existsByType(PaymentGatewayType type);

    /** Désactive tous les gateways avant d'en activer un seul */
    @Modifying
    @Query("UPDATE PaymentGatewayConfig g SET g.active = false")
    void deactivateAll();
}
