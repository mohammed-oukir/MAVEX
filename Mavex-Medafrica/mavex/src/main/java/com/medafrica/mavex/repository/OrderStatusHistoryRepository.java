package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.logistics.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /** Tout l'historique d'un order, trié du plus récent au plus ancien */
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(Long orderId);

    /** Toutes les actions d'un utilisateur (audit) */
    List<OrderStatusHistory> findByChangedByIdOrderByChangedAtDesc(Long userId);
}