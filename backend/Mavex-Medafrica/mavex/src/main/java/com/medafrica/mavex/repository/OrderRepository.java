package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByHawb(String hawb);

    boolean existsByHawb(String hawb);

    boolean existsByHawbAndShipmentMawb(String hawb, String mawb);

    Optional<Order> findByPaymentToken(String paymentToken);

    List<Order> findByShipmentId(Long shipmentId);

    /** Variante fetch-join pour l'affichage (client, shipment, shipment.shipper) — utilisée par getByShipment(). */
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.client " +
           "LEFT JOIN FETCH o.shipment s " +
           "LEFT JOIN FETCH s.shipper " +
           "WHERE o.shipment.id = :shipmentId")
    List<Order> findByShipmentIdWithRelations(@Param("shipmentId") Long shipmentId);

    List<Order> findByClientId(Long clientId);

    List<Order> findByStatus(OrderStatus status);

///////////////////////////////////////////////////////////////////////////
List<Order> findAllByHawbIn(List<String> hawbs);
 long countByClientId(Long clientId);




    @Query("SELECT o FROM Order o WHERE o.shipment.id = :shipmentId AND o.status = :status")
    List<Order> findByShipmentIdAndStatus(@Param("shipmentId") Long shipmentId,
                                          @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shipment.id = :shipmentId")
    long countByShipmentId(@Param("shipmentId") Long shipmentId);

    /* ── Dashboard queries ─────────────────────────────────── */

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT o.shipment.id, COUNT(o) FROM Order o WHERE o.shipment.id IN :shipmentIds GROUP BY o.shipment.id")
    List<Object[]> countByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID'")
    BigDecimal sumPaidAmount();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN ('CREATED', 'EMAIL_SENT', 'EMAIL_OUTDATED')")
    BigDecimal sumPendingAmount();

    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), SUM(o.totalAmount) " +
           "FROM Order o WHERE o.status = 'PAID' AND o.createdAt >= :from " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<Object[]> revenueByMonth(@Param("from") LocalDateTime from);

    long countByStatus(OrderStatus status);

    long countByStatusIn(List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('CREATED', 'EMAIL_SENT', 'EMAIL_OUTDATED') AND o.updatedAt <= :threshold")
    long countOverduePayments(@Param("threshold") LocalDateTime threshold);
}