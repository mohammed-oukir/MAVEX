package com.medafrica.mavex.model.permission;

import jakarta.persistence.*;
import lombok.*;

/**
 * Catalogue des permissions disponibles dans le systeme (module + action).
 * Alimente la liste que l'ADMIN peut accorder a un AGENT.
 * Table BDD : permission_catalog
 */
@Entity
@Table(
    name = "permission_catalog",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_permission_catalog_module_action",
        columnNames = { "module", "action" }
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ex: "ORDERS", "SHIPMENTS" */
    @Column(nullable = false)
    private String module;

    /** ex: "CREATE", "SEND_EMAIL" */
    @Column(nullable = false)
    private String action;

    /** Libelle lisible affiche a l'ADMIN, ex: "Envoyer un email de paiement" */
    @Column(nullable = false)
    private String label;

    /** Precision contextuelle optionnelle, ex: "Utilise dans : detail d'un shipment" */
    @Column(name = "context_note")
    private String contextNote;
}
