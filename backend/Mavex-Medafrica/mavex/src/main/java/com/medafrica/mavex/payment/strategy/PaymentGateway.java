package com.medafrica.mavex.payment.strategy;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;

/**
 * Contrat commun pour tous les gateways de paiement.
 * Chaque implémentation (PayPal, Stripe, CMI) gère son propre SDK.
 * Le PaymentServiceImpl choisit l'implémentation selon le gateway actif.
 */
public interface PaymentGateway {

    /** Identifie quel gateway cette implémentation gère */
    PaymentGatewayType getType();

    /**
     * Crée une session/order de paiement côté gateway.
     * Retourne l'URL d'approbation + la référence gateway (ex: PayPal order ID).
     *
     * @param order     l'order MAVEX à payer
     * @param config    la config du gateway (clés API, mode TEST/PROD)
     * @param returnUrl URL de retour après paiement réussi (backend)
     * @param cancelUrl URL de retour après annulation (frontend)
     */
    GatewayInitiateResult initiatePayment(Order order, PaymentGatewayConfig config,
                                          String returnUrl, String cancelUrl);

    /**
     * Capture un paiement approuvé (spécifique PayPal — Stripe capture automatiquement).
     * Retourne le statut final : "COMPLETED", "FAILED", etc.
     *
     * @param gatewayRef ID de la transaction côté gateway (PayPal Order ID)
     * @param config     config du gateway
     */
    String capturePayment(String gatewayRef, PaymentGatewayConfig config);
}
