package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByMawb(String mawb);

    boolean existsByMawb(String mawb);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}