package com.medafrica.mavex.model.logistics;

import com.medafrica.mavex.model.enums.DutyChangeEntityType;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Journal de chaque modification reelle de duty rate, au niveau
 * Shipment (propagation) ou Order (individuel).
 * Table BDD : duty_change_history
 */
@Entity
@Table(name = "duty_change_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DutyChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHIPMENT (propagation en masse) ou ORDER (modif individuelle) */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private DutyChangeEntityType entityType;

    /** Renseigne si entityType = SHIPMENT */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    /** Renseigne si entityType = ORDER */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "old_duty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal oldDutyRate;

    @Column(name = "new_duty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal newDutyRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
