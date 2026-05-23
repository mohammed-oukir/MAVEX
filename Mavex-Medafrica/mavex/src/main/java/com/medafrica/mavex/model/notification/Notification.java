package com.medafrica.mavex.model.notification;

import com.medafrica.mavex.model.enums.NotificationLevel;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Alertes internes affichees dans le dashboard de l'agent ou admin.
 *
 * Exemples :
 * INFO : "Import termine : 16 lignes importees"
 * WARNING : "1 ligne ignoree - HAWB deja existant"
 * ERROR : "Echec envoi email a sarah@example.com - SMTP timeout"
 *
 * ⚠️ FIX : "read" est un mot reserve MySQL/MariaDB
 * → colonne renommee "is_read" via @Column(name = "is_read")
 * → le champ Java garde le nom "read" pour la lisibilite
 *
 * Table BDD : notifications
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    // ── Clé primaire ─────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Destinataire ──────────────────────────────────────────────────────────
    /** FK → users.id — agent ou admin destinataire de la notification */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Contenu ───────────────────────────────────────────────────────────────
    /** INFO / WARNING / ERROR */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationLevel level;

    /** Titre court ex: "Import termine" */
    @Column(nullable = false, length = 200)
    private String title;

    /** Message detaille ex: "16 lignes importees, 1 ignoree (HAWB doublon)" */
    @Column(nullable = false, length = 1000)
    private String message;

    /** Lien cliquable ex: /shipments/1 ou /import-logs/3 */
    @Column(length = 300)
    private String link;

    // ── État lecture ──────────────────────────────────────────────────────────
    /**
     * ⚠️ "read" est un mot réservé MySQL → colonne nommée "is_read"
     * false = non lue (badge rouge dans le dashboard)
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    // ── Horodatages ──────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Date ou l'agent a clique sur la notification */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ── Méthode utilitaire ────────────────────────────────────────────────────
    /** Marquer comme lue — appele quand l'agent clique sur la notif */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
