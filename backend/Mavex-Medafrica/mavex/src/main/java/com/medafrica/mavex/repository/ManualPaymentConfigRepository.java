package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.payment.ManualPaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualPaymentConfigRepository extends JpaRepository<ManualPaymentConfig, Long> {
}
