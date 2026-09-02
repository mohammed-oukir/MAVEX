package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.DutyChangeEntityType;
import com.medafrica.mavex.model.logistics.DutyChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DutyChangeHistoryRepository
        extends JpaRepository<DutyChangeHistory, Long>, JpaSpecificationExecutor<DutyChangeHistory> {

    /** Historique des changements de duty rate faits directement sur le Shipment */
    List<DutyChangeHistory> findByShipmentIdAndEntityTypeOrderByChangedAtDesc(
            Long shipmentId, DutyChangeEntityType entityType);

    /**
     * Historique des changements de duty rate faits sur les Orders
     * appartenant a ce Shipment (pas de derived query possible : le lien
     * passe par order.shipment.id, une jointure explicite est necessaire).
     */
    @Query("SELECT h FROM DutyChangeHistory h " +
           "WHERE h.entityType = :entityType " +
           "AND h.order.shipment.id = :shipmentId " +
           "ORDER BY h.changedAt DESC")
    List<DutyChangeHistory> findOrderChangesByShipmentId(
            @Param("shipmentId") Long shipmentId,
            @Param("entityType") DutyChangeEntityType entityType);
}
