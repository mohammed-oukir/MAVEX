package com.medafrica.mavex.model.security;

import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import jakarta.persistence.*;
import lombok.*;

/**
 * Permission individuelle d'un agent sur un module/action.
 * Un enregistrement = une case à cocher dans l'interface d'administration.
 * Les ADMIN n'ont jamais d'enregistrements ici : leur accès est total par définition.
 */
@Entity
@Table(
    name = "agent_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_perm_user_module_action",
        columnNames = {"user_id", "module", "action"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionAction action;

    @Column(nullable = false)
    private boolean granted;
}
