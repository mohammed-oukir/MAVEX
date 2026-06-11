package com.medafrica.mavex.model.logistics;

import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Colis individuel d'un destinataire, identifie par le HAWB.
 * Entite centrale du systeme Mavex. Chaque ligne Excel = un Order.
 *
 * Colonnes Excel mappees :
 *   Connote #             -> hawb
 *   Goods Description     -> goodsDescription
 *   Number of Items       -> numberOfItems
 *   Shipment Weight       -> shipmentWeight
 *   Customs Value         -> customsValue
 *   Customs Currency Code -> customsCurrency
 *
 * Table BDD : orders
 */
@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Colonne Excel "Connote #" ex: 013090009953 */
    @Column(unique = true, nullable = false)
    private String hawb;

    /** Colonne Excel "Goods Description" ex: 2 Handmade carpets */
    @Column(name = "goods_description")
    private String goodsDescription;

    /** Code tarif douanier US ex: 6913.90.1000, 5702.10.1000 */
    @Column(name = "htsus_code")
    private String htsusCode;

    /** Colonne Excel "Number of Items" - nombre d'articles */
    @Column(name = "number_of_items")
    private Integer numberOfItems;

    /** Colonne Excel "Shipment Weight" - poids manifeste en KG */
    @Column(name = "shipment_weight", precision = 10, scale = 3)
    private BigDecimal shipmentWeight;

    /** Poids brut CBP Entry Summary en KG */
    @Column(name = "gross_weight", precision = 10, scale = 3)
    private BigDecimal grossWeight;

    /** Quantite nette HTSUS */
    @Column(name = "net_quantity", precision = 10, scale = 3)
    private BigDecimal netQuantity;

    /** Quantite manifeste CBP */
    @Column(name = "manifest_qty")
    private Integer manifestQty;

    /** Colonne Excel "Customs Value" - base du calcul duty en USD */
    @Column(name = "customs_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal customsValue;

    /** Colonne Excel "Customs Currency Code" - toujours USD */
    @Column(name = "customs_currency", length = 3)
    @Builder.Default
    private String customsCurrency = "USD";

    /** Taux douanier - 10% par defaut Section 122 Entry Summary */
    @Column(name = "duty_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal dutyRate = new BigDecimal("0.10");

    /** Auto-calcule : customsValue x dutyRate via @PrePersist */
    @Column(name = "duty_amount", precision = 12, scale = 2)
    private BigDecimal dutyAmount;

    /** Frais bancaires additionnels */
    @Column(name = "bank_charges", precision = 10, scale = 2)
    private BigDecimal bankCharges;

    /** Auto-calcule : dutyAmount + bankCharges via @PrePersist */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Valeur CBP declaree dans l'Entry Summary */
    @Column(name = "entered_value", precision = 12, scale = 2)
    private BigDecimal enteredValue;

    /** CREATED -> EMAIL_SENT -> PENDING_PAYMENT -> PAID -> IN_DELIVERY -> DELIVERED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    /** UUID unique pour /pay/{token} - prive par client */
    @Column(name = "payment_token", unique = true)
    private String paymentToken;

    /** Expiration lien paiement +7 jours */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /** FK -> shipments.id - MAWB parent */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    /** FK -> clients.id - destinataire americain */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "email_sent_count", nullable = false)
    @Builder.Default
    private int emailSentCount = 0;

    /** Email utilisé lors du dernier envoi — comparé à client.email pour détecter EMAIL_OUTDATED */
    @Column(name = "email_sent_to_address")
    private String emailSentToAddress;

    /** Raison du statut EMAIL_OUTDATED — affiché dans l'UI */
    @Column(name = "email_outdated_reason")
    private String emailOutdatedReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    /** Colonne Excel "Alternate Reference" */
@Column(name = "alternate_reference")
private String alternateReference;



    @PrePersist
    @PreUpdate
    public void calculateAmounts() {
        if (customsValue != null && dutyRate != null) {
            this.dutyAmount  = customsValue.multiply(dutyRate);
            BigDecimal charges = (bankCharges != null) ? bankCharges : BigDecimal.ZERO;
            this.totalAmount = dutyAmount.add(charges);
        }
    }

    public void generatePaymentToken() {
        this.paymentToken   = UUID.randomUUID().toString();
        this.tokenExpiresAt = LocalDateTime.now().plusDays(7);
    }

    public boolean isTokenValid() {
        return paymentToken != null
            && tokenExpiresAt != null
            && LocalDateTime.now().isBefore(tokenExpiresAt);
    }
}