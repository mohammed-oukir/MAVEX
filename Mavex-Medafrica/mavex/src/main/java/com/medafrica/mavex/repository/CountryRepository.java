package com.medafrica.mavex.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.model.imports.ImportLog;

public interface CountryRepository extends JpaRepository<Country, String> {
    Optional<Country> findByCode(String Code);
    

    Optional<ImportLog> findByNameIgnoreCase(String countryCode);
}