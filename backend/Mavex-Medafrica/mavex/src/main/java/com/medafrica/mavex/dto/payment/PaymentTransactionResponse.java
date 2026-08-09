package com.medafrica.mavex.dto.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentTransactionResponse {

    private Long              id;
    private String            gatewayRef;
    private PaymentGatewayType gateway;
    private BigDecimal        amount;
    private String            currency;
    private PaymentStatus     status;
    private String            ipAddress;
    private LocalDateTime     paidAt;
    private LocalDateTime     createdAt;

    // Résumé de la commande liée — pour identification dans une UI de listing
    private Long   orderId;
    private String hawb;
    private String clientFullName;

    // true si un email PAYMENT_CONFIRMED a déjà été envoyé avec succès pour cette
    // commande — pertinent uniquement pour gateway=MANUEL (PayPal l'envoie déjà
    // automatiquement dans markSuccess()).
    private boolean receiptSent;
}
