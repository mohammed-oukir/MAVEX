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

    /** TEST ou PRODUCTION */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentGatewayMode mode = PaymentGatewayMode.TEST;

    /** Un seul true à la fois — géré dans GatewayConfigServiceImpl.activate() */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

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

