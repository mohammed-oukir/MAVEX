package com.medafrica.mavex.model.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Configuration PayPal. Reliée à PaymentGatewayConfig (type=PAYPAL) pour l'activation.
 * Table BDD : paypal_configs
 */
@Entity
@Table(name = "paypal_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaypalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant public PayPal (client ID), en clair */
    @Column(name = "client_id")
    private String clientId;

    /** Client secret PayPal, chiffré AES-256 — déchiffré à la volée à l'usage */
    @Column(name = "client_secret_encrypted")
    private String clientSecretEncrypted;

    /** Identifiant du webhook PayPal, pas un secret */
    @Column(name = "webhook_id")
    private String webhookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGatewayMode mode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
