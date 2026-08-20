package com.medafrica.mavex.model.permission;

import com.medafrica.mavex.model.enums.PermissionActionType;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Journal des octrois/retraits de permissions AGENT.
 * Table BDD : permission_audit_logs
 */
@Entity
@Table(name = "permission_audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionCatalog permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private PermissionActionType actionType;

    /** ADMIN a l'origine de l'octroi/retrait */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @CreationTimestamp
    @Column(name = "performed_at", updatable = false)
    private LocalDateTime performedAt;
}
