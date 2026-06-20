package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByOrderId(Long orderId);

    List<EmailLog> findByStatus(EmailStatus status);

    void deleteByOrderId(Long orderId);

    Optional<EmailLog> findByMessageId(String messageId);

    Optional<EmailLog> findTopByOrderIdAndStatusOrderBySentAtDesc(Long orderId, EmailStatus status);

}