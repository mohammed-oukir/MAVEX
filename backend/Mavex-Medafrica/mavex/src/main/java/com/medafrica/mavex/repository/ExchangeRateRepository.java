package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.finance.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByIsActiveTrueAndFromCurrencyAndToCurrency(
            String fromCurrency, String toCurrency);

    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndEffectiveDate(
            String fromCurrency, String toCurrency, LocalDate effectiveDate);

    List<ExchangeRate> findAllByIsActiveTrue();

    @Modifying
    @Query("UPDATE ExchangeRate e SET e.isActive = false WHERE e.fromCurrency = :from AND e.toCurrency = :to")
    void deactivateAll(@Param("from") String fromCurrency, @Param("to") String toCurrency);
}
