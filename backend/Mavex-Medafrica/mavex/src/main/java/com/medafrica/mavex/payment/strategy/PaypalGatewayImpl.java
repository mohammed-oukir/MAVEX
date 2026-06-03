package com.medafrica.mavex.payment.strategy;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Implémentation PayPal du strategy PaymentGateway.
 * Utilise le PayPal Checkout SDK (checkout-sdk 1.0.5).
 *
 * Flux :
 *  1. initiatePayment  → crée un Order PayPal → retourne l'URL d'approbation
 *  2. capturePayment   → capture l'Order PayPal approuvé → retourne "COMPLETED" ou "FAILED"
 */
@Slf4j
@Component
public class PaypalGatewayImpl implements PaymentGateway {

    @Override
    public PaymentGatewayType getType() {
        return PaymentGatewayType.PAYPAL;
    }

    /* ── Initiation ──────────────────────────────────────── */

    @Override
    public GatewayInitiateResult initiatePayment(Order order, PaymentGatewayConfig config,
                                                  String returnUrl, String cancelUrl) {
        try {
            PayPalHttpClient client = buildClient(config);

            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer("return=representation");
            request.requestBody(buildOrderRequest(order, returnUrl, cancelUrl));

            com.paypal.orders.Order paypalOrder = client.execute(request).result();

            String paypalOrderId = paypalOrder.id();
            String approveUrl    = paypalOrder.links().stream()
                    .filter(l -> "approve".equals(l.rel()))
                    .map(LinkDescription::href)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("PayPal : lien d'approbation introuvable"));

            log.info("PayPal order créé : {} → {}", paypalOrderId, approveUrl);
            return new GatewayInitiateResult(approveUrl, paypalOrderId);

        } catch (IOException e) {
            log.error("PayPal initiatePayment error : {}", e.getMessage());
            throw new RuntimeException("Erreur PayPal lors de la création du paiement : " + e.getMessage(), e);
        }
    }

    /* ── Capture ─────────────────────────────────────────── */

    @Override
    public String capturePayment(String paypalOrderId, PaymentGatewayConfig config) {
        try {
            PayPalHttpClient client = buildClient(config);

            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.requestBody(new OrderActionRequest());

            com.paypal.orders.Order captured = client.execute(request).result();
            String status = captured.status();

            log.info("PayPal order capturé : {} → status={}", paypalOrderId, status);
            return status;

        } catch (IOException e) {
            log.error("PayPal capturePayment error : {}", e.getMessage());
            throw new RuntimeException("Erreur PayPal lors de la capture : " + e.getMessage(), e);
        }
    }

    /* ── Helpers ─────────────────────────────────────────── */

    private PayPalHttpClient buildClient(PaymentGatewayConfig config) {
        PayPalEnvironment env = config.getMode() == PaymentGatewayMode.TEST
                ? new PayPalEnvironment.Sandbox(config.getApiKey(), config.getSecretKey())
                : new PayPalEnvironment.Live(config.getApiKey(), config.getSecretKey());
        return new PayPalHttpClient(env);
    }

    private OrderRequest buildOrderRequest(Order order, String returnUrl, String cancelUrl) {
        String currency = order.getCustomsCurrency() != null ? order.getCustomsCurrency() : "USD";
        String amount   = order.getTotalAmount().toPlainString();

        PurchaseUnitRequest unit = new PurchaseUnitRequest()
                .description(order.getGoodsDescription() != null
                        ? order.getGoodsDescription() : "Frais de douane")
                .customId(String.valueOf(order.getId()))
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(currency)
                        .value(amount));

        ApplicationContext appContext = new ApplicationContext()
                .brandName("Med Africa Logistics")
                .landingPage("BILLING")
                .userAction("PAY_NOW")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl);

        return new OrderRequest()
                .checkoutPaymentIntent("CAPTURE")
                .purchaseUnits(List.of(unit))
                .applicationContext(appContext);
    }
}
