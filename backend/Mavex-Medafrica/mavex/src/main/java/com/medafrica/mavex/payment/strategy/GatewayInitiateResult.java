package com.medafrica.mavex.payment.strategy;

/**
 * Résultat de l'initiation d'un paiement.
 * redirectUrl  : URL vers laquelle rediriger le client (page PayPal/Stripe/CMI).
 * gatewayRef   : ID de la transaction côté gateway (ex: PayPal Order ID).
 */
public record GatewayInitiateResult(String redirectUrl, String gatewayRef) {}
