package com.medafrica.mavex.model.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chaque tentative de paiement (CMI, Stripe, PayPal).
 * Un Order peut avoir plusieurs transactions (retry apres echec).
 * Table BDD : payment_transactions
 */
@Entity
@Table(name = "payment_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID retourne par le gateway ex: pi_3OxYZ (Stripe), CMI-20260511-001 */
    @Column(name = "gateway_ref")
    private String gatewayRef;

    /** Provider : CMI | STRIPE | PAYPAL */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGatewayType gateway;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    /** INITIATED -> SUCCESS / FAILED / CANCELLED -> REFUNDED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    /** Payload JSON brut du webhook - conserve pour audit et debug */
    @Lob
    @Column(name = "webhook_payload", columnDefinition = "TEXT")
    private String webhookPayload;

    /** IP du client au moment du paiement */
    @Column(name = "ip_address")
    private String ipAddress;

    /** Date confirmation par webhook */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** FK -> orders.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}   