package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.airline.AirlineDTO;
import com.medafrica.mavex.model.logistics.Airline;
import com.medafrica.mavex.repository.AirlineRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirlineServiceImpl {

    private final AirlineRepository airlineRepository;

    public List<Airline> findAll() {
        return airlineRepository.findAll();
    }

    public Optional<Airline> findByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return Optional.empty();
        // Extraire les 3 premiers chiffres du MAWB (ex: "147-44480365" -> "147")
        String clean = prefix.replaceAll("[^0-9]", "");
        if (clean.length() < 3) return Optional.empty();
        return airlineRepository.findById(clean.substring(0, 3));
    }

    public List<Airline> search(String name) {
        return airlineRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Airline create(AirlineDTO dto) {
        if (airlineRepository.existsById(dto.getPrefix())) {
            throw new IllegalArgumentException("Une compagnie avec le préfixe '" + dto.getPrefix() + "' existe déjà.");
        }
        return airlineRepository.save(toEntity(dto));
    }

    @Transactional
    public Airline update(String prefix, AirlineDTO dto) {
        Airline airline = airlineRepository.findById(prefix)
                .orElseThrow(() -> new EntityNotFoundException("Compagnie introuvable : " + prefix));
        airline.setName(dto.getName());
        airline.setIataCode(dto.getIataCode());
        airline.setIcaoCode(dto.getIcaoCode());
        airline.setCountry(dto.getCountry());
        airline.setMode(dto.getMode() != null ? dto.getMode() : "AIR");
        return airlineRepository.save(airline);
    }

    @Transactional
    public void delete(String prefix) {
        if (!airlineRepository.existsById(prefix))
            throw new EntityNotFoundException("Compagnie introuvable : " + prefix);
        airlineRepository.deleteById(prefix);
    }

    private Airline toEntity(AirlineDTO dto) {
        return Airline.builder()
                .prefix(dto.getPrefix())
                .name(dto.getName())
                .iataCode(dto.getIataCode())
                .icaoCode(dto.getIcaoCode())
                .country(dto.getCountry())
                .mode(dto.getMode() != null ? dto.getMode() : "AIR")
                .build();
    }
}
