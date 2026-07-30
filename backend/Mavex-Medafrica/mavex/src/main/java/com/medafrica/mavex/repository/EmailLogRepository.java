package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByOrderId(Long orderId);

    List<EmailLog> findByStatus(EmailStatus status);

    void deleteByOrderId(Long orderId);

    Optional<EmailLog> findByMessageId(String messageId);

    Optional<EmailLog> findTopByOrderIdAndStatusOrderBySentAtDesc(Long orderId, EmailStatus status);

    /**
     * Équivalent groupé de findTopByOrderIdAndStatusOrderBySentAtDesc appelée en boucle :
     * pour chaque orderId de la liste, ne retourne que l'EmailLog avec le sentAt maximum
     * parmi ceux au status donné. Un seul aller-retour DB pour toute la page.
     */
    @Query("SELECT e FROM EmailLog e WHERE e.order.id IN :orderIds AND e.status = :status " +
           "AND e.sentAt = (SELECT MAX(e2.sentAt) FROM EmailLog e2 " +
           "WHERE e2.order.id = e.order.id AND e2.status = :status)")
    List<EmailLog> findLastSentByOrderIds(@Param("orderIds") List<Long> orderIds,
                                            @Param("status") EmailStatus status);

    long countBySentAtBetween(LocalDateTime start, LocalDateTime end);

    long countByDeliveredAtBetween(LocalDateTime start, LocalDateTime end);

    long countByOpenedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByBouncedAtBetween(LocalDateTime start, LocalDateTime end);

}