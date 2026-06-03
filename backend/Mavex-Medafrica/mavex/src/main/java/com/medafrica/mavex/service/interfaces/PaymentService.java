package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.payment.PaymentInitiateResponse;

public interface PaymentService {

    /**
     * Initie le paiement pour l'order associé au token.
     * Retourne l'URL de redirection vers PayPal/Stripe/CMI.
     */
    PaymentInitiateResponse initiatePayment(String paymentToken);

    /**
     * Capture un paiement PayPal après approbation du client.
     * Appelé depuis le returnUrl PayPal.
     *
     * @param paypalOrderId ID PayPal reçu dans le paramètre ?token=
     */
    void capturePaypalPayment(String paypalOrderId);
}
