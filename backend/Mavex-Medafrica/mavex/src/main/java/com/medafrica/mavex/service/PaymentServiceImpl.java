package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.payment.PaymentInitiateResponse;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.payment.strategy.GatewayInitiateResult;
import com.medafrica.mavex.payment.strategy.PaymentGateway;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.PaymentGatewayConfigRepository;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.service.interfaces.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository               orderRepo;
    private final PaymentGatewayConfigRepository gatewayConfigRepo;
    private final PaymentTransactionRepository   txRepo;

    /** Toutes les implémentations de PaymentGateway injectées automatiquement */
    private final List<PaymentGateway> gateways;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /* ── Initiation ──────────────────────────────────────── */

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(String paymentToken) {

        // 1. Charger l'order
        Order order = orderRepo.findByPaymentToken(paymentToken)
                .orElseThrow(() -> new EntityNotFoundException("Token de paiement invalide."));

        if (!order.isTokenValid()) {
            throw new IllegalStateException("Le lien de paiement a expiré.");
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cet order a déjà été payé.");
        }

        // 2. Lire le gateway actif
        PaymentGatewayConfig config = gatewayConfigRepo.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun gateway de paiement actif. Contactez l'administrateur."));

        // 3. Choisir l'implémentation (Strategy Pattern)
        PaymentGateway gateway = gateways.stream()
                .filter(g -> g.getType() == config.getType())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Gateway non implémenté : " + config.getType()));

        // 4. Créer la transaction en base (INITIATED)
        PaymentTransaction tx = PaymentTransaction.builder()
                .order(order)
                .gateway(config.getType())
                .amount(order.getTotalAmount())
                .currency(order.getCustomsCurrency() != null ? order.getCustomsCurrency() : "USD")
                .status(PaymentStatus.INITIATED)
                .build();
        tx = txRepo.save(tx);

        // 5. Construire les URLs de retour
        String returnUrl = baseUrl + "/api/payments/paypal/capture";
        String cancelUrl = frontendUrl + "/pay/" + paymentToken + "?cancelled=true";

        // 6. Initier le paiement côté gateway
        GatewayInitiateResult result = gateway.initiatePayment(order, config, returnUrl, cancelUrl);

        // 7. Stocker la référence gateway dans la transaction
        tx.setGatewayRef(result.gatewayRef());
        txRepo.save(tx);

        // 8. Passer l'order en PENDING_PAYMENT
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepo.save(order);

        log.info("Paiement initié — order={} gateway={} ref={}",
                order.getHawb(), config.getType(), result.gatewayRef());

        return PaymentInitiateResponse.builder()
                .redirectUrl(result.redirectUrl())
                .transactionId(tx.getId())
                .gateway(config.getType().name())
                .build();
    }

    /* ── Capture PayPal ──────────────────────────────────── */

    @Override
    @Transactional
    public void capturePaypalPayment(String paypalOrderId) {

        // 1. Retrouver la transaction par la ref PayPal
        PaymentTransaction tx = txRepo.findByGatewayRef(paypalOrderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transaction introuvable pour PayPal order : " + paypalOrderId));

        // 2. Charger la config PayPal active
        PaymentGatewayConfig config = gatewayConfigRepo.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Aucun gateway actif."));

        // 3. Choisir l'implémentation PayPal
        PaymentGateway gateway = gateways.stream()
                .filter(g -> g.getType() == config.getType())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Gateway PayPal non trouvé."));

        try {
            // 4. Capturer le paiement
            String status = gateway.capturePayment(paypalOrderId, config);

            if ("COMPLETED".equalsIgnoreCase(status)) {
                // 5a. Succès → mise à jour transaction + order
                tx.setStatus(PaymentStatus.SUCCESS);
                tx.setPaidAt(java.time.LocalDateTime.now());
                txRepo.save(tx);

                Order order = tx.getOrder();
                order.setStatus(OrderStatus.PAID);
                orderRepo.save(order);

                log.info("Paiement PayPal confirmé — order={} paypalRef={}",
                        order.getHawb(), paypalOrderId);
            } else {
                // 5b. Échec
                tx.setStatus(PaymentStatus.FAILED);
                txRepo.save(tx);
                log.warn("Paiement PayPal non complété — status={} ref={}", status, paypalOrderId);
            }

        } catch (Exception e) {
            tx.setStatus(PaymentStatus.FAILED);
            txRepo.save(tx);
            log.error("Erreur capture PayPal — ref={} : {}", paypalOrderId, e.getMessage());
            throw e;
        }
    }
}
