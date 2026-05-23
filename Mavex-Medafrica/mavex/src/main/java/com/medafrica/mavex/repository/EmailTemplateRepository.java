// ── EmailTemplateRepository
package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    /** Cherche le template actif pour un type donné */
    Optional<EmailTemplate> findByTypeAndActiveTrue(NotificationType type);
}


// ── EmailLogRepository
