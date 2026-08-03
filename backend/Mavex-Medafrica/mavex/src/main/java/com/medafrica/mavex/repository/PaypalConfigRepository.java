package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.payment.PaypalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaypalConfigRepository extends JpaRepository<PaypalConfig, Long> {
}
