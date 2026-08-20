package com.medafrica.mavex.model.permission;

import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Permission accordee a un AGENT (deny by default : la simple presence
 * d'une ligne = permission accordee, pas de champ booleen "granted").
 * Table BDD : agent_permissions
 */
@Entity
@Table(
    name = "agent_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_agent_permissions_agent_permission",
        columnNames = { "agent_id", "permission_id" }
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionCatalog permission;

    /** ADMIN ayant accorde cette permission */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", updatable = false)
    private LocalDateTime grantedAt;
}
