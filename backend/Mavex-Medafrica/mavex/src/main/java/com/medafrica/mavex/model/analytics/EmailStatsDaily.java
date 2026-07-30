package com.medafrica.mavex.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Résumé global des emails pour une journée donnée.
 * Table BDD : email_stats_daily
 */
@Entity
@Table(
    name = "email_stats_daily",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_email_stats_daily_stat_date",
        columnNames = {"stat_date"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailStatsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Journée concernée par ce résumé */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "emails_sent", nullable = false)
    @Builder.Default
    private Integer emailsSent = 0;

    @Column(name = "emails_delivered", nullable = false)
    @Builder.Default
    private Integer emailsDelivered = 0;

    @Column(name = "emails_opened", nullable = false)
    @Builder.Default
    private Integer emailsOpened = 0;

    @Column(name = "emails_bounced", nullable = false)
    @Builder.Default
    private Integer emailsBounced = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
