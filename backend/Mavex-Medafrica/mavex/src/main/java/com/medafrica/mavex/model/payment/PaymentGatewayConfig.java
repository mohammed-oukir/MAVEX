package com.medafrica.mavex.model.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Configuration d'un gateway de paiement gérée depuis l'UI admin.
 * Un seul gateway peut avoir active=true à la fois.
 * Les clés sont stockées en clair — chiffrement à ajouter en production.
 * Table BDD : payment_gateway_configs
 */
@Entity
@Table(name = "payment_gateway_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** STRIPE | PAYPAL | CMI */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGatewayType type;

    /** Nom affiché dans l'UI ex: "Stripe Production" */
    @Column(nullable = false)
    private String name;

    /**
     * Clé publique / identifiant marchand
     * Stripe  : publishable key (pk_live_...)
     * PayPal  : client ID
     * CMI     : merchant ID
     */
    @Column(name = "api_key")
    private String apiKey;

    /**
     * Clé secrète
     * Stripe  : secret key (sk_live_...)
     * PayPal  : client secret
     * CMI     : clé secrète HMACg
     */
    @Column(name = "secret_key")
    private String secretKey;

    /**
     * Secret de vérification webhook
     * Stripe  : whsec_...
     * PayPal  : webhook ID
     * CMI     : clé HMAC post-back
     */
    @Column(name = "webhook_secret")
    private String webhookSecret;

    /** TEST ou PRODUCTION */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentGatewayMode mode = PaymentGatewayMode.TEST;

    /** Un seul true à la fois — géré dans GatewayConfigServiceImpl.activate() */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    /** Devises acceptées ex: USD,EUR,MAD */
    @Column(name = "supported_currencies", length = 100)
    @Builder.Default
    private String supportedCurrencies = "USD";

    /** Notes internes pour l'admin */
    @Column(length = 500)
    private String description;

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
