package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByOrderId(Long orderId);

    List<EmailLog> findByStatus(EmailStatus status);
}