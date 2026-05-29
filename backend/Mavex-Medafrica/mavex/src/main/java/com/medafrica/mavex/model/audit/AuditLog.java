package com.medafrica.mavex.model.audit;

import com.medafrica.mavex.model.enums.AuditAction;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Journal global de TOUTES les actions du systeme Mavex.
 * Permet de savoir qui a fait quoi, quand, sur quoi.
 * user = null si action automatique (webhook, scheduler).
 * Table BDD : audit_logs
 */
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null si action automatique (webhook Stripe, scheduler retry...) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Type d'action effectuee */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /** Nom de l'entite concernee ex: "Order", "SmtpConfig", "User" */
    @Column(name = "entity_name")
    private String entityName;

    /** ID de l'objet concerne */
    @Column(name = "entity_id")
    private Long entityId;

    /** JSON de l'objet AVANT modification (null si creation) */
    @Lob
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** JSON de l'objet APRES modification */
    @Lob
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Description lisible ex: "Order #013090009953 -> EMAIL_SENT" */
    @Column(length = 500)
    private String description;

    /** IP de l'utilisateur */
    @Column(name = "ip_address")
    private String ipAddress;

    /** Navigateur utilise */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}