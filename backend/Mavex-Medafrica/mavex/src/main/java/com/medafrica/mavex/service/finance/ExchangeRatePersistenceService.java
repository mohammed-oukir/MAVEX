package com.medafrica.mavex.service.finance;

import com.medafrica.mavex.model.finance.ExchangeRate;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Isole la transaction deactivateAll + save dans un bean Spring séparé
 * pour que @Transactional soit honoré (évite le problème de self-invocation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRatePersistenceService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional
    public void deactivateAndSave(String fromCurrency, String toCurrency,
                                  BigDecimal rate, LocalDate effectiveDate,
                                  String source, User importedBy) {
        ExchangeRate entity = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndEffectiveDate(fromCurrency, toCurrency, effectiveDate)
            .orElse(null);

        if (entity != null) {
            // Même paire + même date : mise à jour en place, pas de nouvel INSERT
            entity.setRate(rate);
            entity.setSource(source);
            entity.setIsActive(true);
            entity.setImportedBy(importedBy);
            entity.setImportedAt(LocalDateTime.now());
        } else {
            // Nouvelle date : désactiver l'ancien taux actif puis insérer
            exchangeRateRepository.deactivateAll(fromCurrency, toCurrency);
            entity = ExchangeRate.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .effectiveDate(effectiveDate)
                .source(source)
                .isActive(true)
                .importedBy(importedBy)
                .importedAt(LocalDateTime.now())
                .build();
        }

        exchangeRateRepository.save(entity);
        log.debug("Taux persisté : {}/{} = {} (effectif le {})", fromCurrency, toCurrency, rate, effectiveDate);
    }

    @Transactional
    public void delete(Long id) {
        exchangeRateRepository.deleteById(id);
    }
}
