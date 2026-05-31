package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByMawb(String mawb);

    boolean existsByMawb(String mawb);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    long countByStatusNot(ShipmentStatus status);

    long countByStatus(ShipmentStatus status);

    Optional<Shipment> findFirstByMawbOrderByCreatedAtDesc(String mawb);

    @Query("SELECT YEAR(s.createdAt), MONTH(s.createdAt), COUNT(s) " +
           "FROM Shipment s WHERE s.createdAt >= :from " +
           "GROUP BY YEAR(s.createdAt), MONTH(s.createdAt) " +
           "ORDER BY YEAR(s.createdAt), MONTH(s.createdAt)")
    List<Object[]> countByMonth(@Param("from") LocalDateTime from);
}