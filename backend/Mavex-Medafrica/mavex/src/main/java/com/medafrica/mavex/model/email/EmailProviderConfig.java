package com.medafrica.mavex.model.email;

import com.medafrica.mavex.model.enums.EmailProviderType;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configuration d'un provider d'envoi email (BREVO ou SMTP).
 * Un seul enregistrement peut avoir active=true a la fois — gere dans EmailProviderConfigService.
 * Secrets (mot de passe SMTP, cle API Brevo) stockes chiffres AES-256 — jamais en clair.
 * Table BDD : email_provider_configs
 */
@Entity
@Table(name = "email_provider_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private EmailProviderType providerType;

    /** Un seul provider actif a la fois — voir EmailProviderConfigService.activate() */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    // --- Champs communs ---

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "from_name", nullable = false)
    private String fromName;

    // --- Champs SMTP (non null si providerType = SMTP) ---

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    /** Mot de passe chiffre AES-256 — dechiffre a la volee dans SmtpEmailProviderService */
    @Column(name = "smtp_password_encrypted")
    private String smtpPasswordEncrypted;

    @Column(name = "smtp_ssl_enabled")
    @Builder.Default
    private boolean smtpSslEnabled = false;

    @Column(name = "smtp_tls_enabled")
    @Builder.Default
    private boolean smtpTlsEnabled = true;

    // --- Champs Brevo (non null si providerType = BREVO) ---

    /** Cle API Brevo chiffree AES-256 — dechiffree a la volee dans BrevoEmailProviderService */
    @Column(name = "brevo_api_key_encrypted")
    private String brevoApiKeyEncrypted;

    /** Token webhook Brevo chiffre AES-256 — utilise pour valider les events entrants */
    @Column(name = "brevo_webhook_token_encrypted")
    private String brevoWebhookTokenEncrypted;

    // --- Audit ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Relation inverse — EmailLog trace quel provider a envoye chaque email.
     * mappedBy correspond au champ emailProviderConfig dans EmailLog.
     */
    @OneToMany(mappedBy = "emailProviderConfig", fetch = FetchType.LAZY)
    private List<EmailLog> emailLogs;
}
