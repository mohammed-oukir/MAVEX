package com.medafrica.mavex.model.email;

import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Configuration serveur SMTP.
 * Mot de passe chiffre AES-256.
 * Un seul peut avoir byDefault=true a la fois - gere dans SmtpConfigService.
 * Table BDD : smtp_configs
 */
@Entity
@Table(name = "smtp_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmtpConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Adresse serveur ex: smtp.gmail.com */
    @Column(nullable = false)
    private String host;

    /** Port: 587=TLS | 465=SSL | 25=plain */
    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String username;

    /**
     * Mot de passe chiffre AES-256.
     * Utiliser jasypt ou AttributeConverter personnalise.
     */
    @Column(name = "password_encrypted", nullable = false)
    private String passwordEncrypted;

    @Column(length = 10)
    @Builder.Default
    private String protocol = "smtp";

    @Column(name = "ssl_enabled")
    @Builder.Default
    private boolean sslEnabled = false;

    @Column(name = "tls_enabled")
    @Builder.Default
    private boolean tlsEnabled = true;

    /** Un seul SMTP vrai a la fois - voir SmtpConfigService.setAsDefault() */
    @Column(name = "by_default")
    @Builder.Default
    private boolean byDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}