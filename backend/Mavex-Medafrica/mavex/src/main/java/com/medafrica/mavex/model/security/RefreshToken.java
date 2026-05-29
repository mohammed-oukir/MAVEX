package com.medafrica.mavex.model.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Token JWT de renouvellement de session.
 * Table BDD : refresh_tokens
 */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID unique par session */
    @Column(unique = true, nullable = false)
    private String token;

    /** Expiration +7 jours */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Invalidé manuellement au logout ou reset password */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /** FK -> users.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() { return LocalDateTime.now().isAfter(this.expiresAt); }
    public boolean isValid()   { return !revoked && !isExpired(); }
}