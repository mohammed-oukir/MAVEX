package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.model.enums.EmailProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailProviderConfigRepository extends JpaRepository<EmailProviderConfig, Long> {

    /** Retourne le provider actuellement actif — au plus un seul a la fois */
    Optional<EmailProviderConfig> findByActiveTrue();

    /** Verifie si une config existe deja pour ce type de provider */
    boolean existsByProviderType(EmailProviderType providerType);

    /** Retourne la config pour un type donne (utile pour upsert dans le service) */
    Optional<EmailProviderConfig> findByProviderType(EmailProviderType providerType);

    /** Desactive tous les providers en une seule requete — appele avant activate() */
    @Modifying
    @Query("UPDATE EmailProviderConfig c SET c.active = false")
    void deactivateAll();
}
