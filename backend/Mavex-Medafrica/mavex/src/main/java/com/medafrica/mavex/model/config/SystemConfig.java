package com.medafrica.mavex.model.config;

import com.medafrica.mavex.model.enums.ConfigType;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Configuration globale et dynamique du systeme Mavex.
 * Modifiable par l'admin depuis le dashboard sans redemarrage.
 *
 * Exemples de cles :
 *   duty.rate.default         = "0.10"   (taux douanier 10%)
 *   payment.token.expiry.days = "7"      (expiration lien paiement)
 *   email.retry.max           = "3"      (max retries email)
 *   email.retry.delay.minutes = "30"     (delai entre retries)
 *   import.duplicate.check    = "true"   (verifier doublons HAWB)
 *   import.file.hash.check    = "true"   (verifier doublon fichier)
 *   payment.gateway.default   = "STRIPE" (gateway par defaut)
 *
 * Table BDD : system_configs
 */
@Entity
@Table(name = "system_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cle unique du parametre ex: duty.rate.default */
    @Column(name = "config_key", unique = true, nullable = false)
    private String configKey;

    /** Valeur du parametre ex: "0.10" */
    @Column(name = "config_value", nullable = false)
    private String configValue;

    /** Description lisible du parametre */
    @Column(length = 500)
    private String description;

    /** Type de la valeur : STRING / NUMBER / BOOLEAN */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigType type;

    /** false = lecture seule - ne peut pas etre modifie depuis le dashboard */
    @Column(nullable = false)
    @Builder.Default
    private boolean editable = true;

    /** Dernier admin qui a modifie ce parametre */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}