package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByHawb(String hawb);

    boolean existsByHawb(String hawb);

    Optional<Order> findByPaymentToken(String paymentToken);

    List<Order> findByShipmentId(Long shipmentId);

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
}