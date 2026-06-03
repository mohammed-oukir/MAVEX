package com.medafrica.mavex.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitiateResponse {

    /** URL vers laquelle rediriger le client (page PayPal) */
    private String redirectUrl;

    /** ID de notre PaymentTransaction en base */
    private Long transactionId;

    /** Nom du gateway utilisé : PAYPAL, STRIPE, CMI */
    private String gateway;
}
