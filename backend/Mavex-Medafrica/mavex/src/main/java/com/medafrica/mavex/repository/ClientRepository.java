package com.medafrica.mavex.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.medafrica.mavex.model.actor.Client;
public interface ClientRepository extends JpaRepository<Client, Long> {
Optional<Client> findByEmail(String email);
 
    boolean existsByEmail(String email);
}




