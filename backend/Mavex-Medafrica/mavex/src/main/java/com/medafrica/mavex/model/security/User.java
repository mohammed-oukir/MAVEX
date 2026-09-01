package com.medafrica.mavex.model.security;

import com.medafrica.mavex.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Agent ou administrateur du système Mavex.
 * Table BDD : users
 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom complet affiché dans l'interface */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Identifiant de connexion - unique en BDD */
    @Column(unique = true, nullable = false)
    private String email;

    /** Mot de passe haché BCrypt - jamais en clair */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** ADMIN ou AGENT - stocké en VARCHAR */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /** false = compte désactivé (soft delete) */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Date du dernier changement de mot de passe. Null = jamais changé depuis la création (aucune restriction appliquée). */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
}