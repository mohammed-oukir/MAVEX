package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.logistics.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirlineRepository extends JpaRepository<Airline, String> {
    List<Airline> findByNameContainingIgnoreCase(String name);
}
