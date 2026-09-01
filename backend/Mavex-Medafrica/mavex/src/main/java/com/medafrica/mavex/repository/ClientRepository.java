package com.medafrica.mavex.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.medafrica.mavex.model.actor.Client;
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Client> findByActiveTrue();

    List<Client> findAllByOrderByCreatedAtDesc();

    long countByActiveTrue();

    long countByActiveFalse();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}




