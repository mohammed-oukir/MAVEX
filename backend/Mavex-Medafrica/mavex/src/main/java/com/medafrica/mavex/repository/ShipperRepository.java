package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.actor.Shipper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ShipperRepository extends JpaRepository<Shipper, Long>, JpaSpecificationExecutor<Shipper> {

    Page<Shipper> findByActiveTrue(Pageable pageable);

    List<Shipper> findAllByOrderByCreatedAtDesc();

    /** Utilisé lors de l'import Excel colonne "Sender Name" */
    Optional<Shipper> findByCompanyNameIgnoreCase(String companyName);

    boolean existsByEmail(String email);

    /** Pour la validation email au UPDATE sans se bloquer soi-même */
    boolean existsByEmailAndIdNot(String email, Long id);
}