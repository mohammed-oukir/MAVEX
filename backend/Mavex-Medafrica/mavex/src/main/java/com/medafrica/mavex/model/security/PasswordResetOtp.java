package com.medafrica.mavex.model.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * OTP 6 chiffres pour reset mot de passe. Expire en 15 min. Usage unique.
 * Table BDD : password_reset_otps
 */
@Entity
@Table(name = "password_reset_otps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code 6 chiffres - String pour garder les zeros de gauche */
    @Column(nullable = false, length = 6)
    private String otp;

    /** Email destinataire du code */
    @Column(nullable = false)
    private String email;

    /** Expiration +15 minutes */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** true apres verification reussie - usage unique */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() { return LocalDateTime.now().isAfter(this.expiresAt); }
    public boolean isValid()   { return !used && !isExpired(); }
}