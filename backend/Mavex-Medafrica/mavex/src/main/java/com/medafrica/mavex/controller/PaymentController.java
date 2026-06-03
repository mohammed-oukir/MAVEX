package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.payment.PaymentInitiateResponse;
import com.medafrica.mavex.dto.payment.PaymentTransactionResponse;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.service.interfaces.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.io.IOException;

/**
 * Endpoints publics du paiement — pas de JWT requis.
 * Tous sous /api/payments/** (déjà en permitAll dans SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService                paymentService;
    private final PaymentTransactionRepository  txRepo;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * POST /api/payments/pay/{token}/initiate
     * Le client clique "Payer maintenant" → on crée la session PayPal.
     * Retourne l'URL de redirection vers PayPal.
     */
    @PostMapping("/pay/{token}/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiate(@PathVariable String token) {
        return ResponseEntity.ok(paymentService.initiatePayment(token));
    }

    /**
     * GET /api/payments/transactions/order/{orderId}
     * Retourne les transactions de paiement d'un order (vue admin).
     */
    @GetMapping("/transactions/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<PaymentTransactionResponse>> getTransactionsByOrder(
            @PathVariable Long orderId) {
        List<PaymentTransactionResponse> result = txRepo
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/payments/paypal/capture?token={paypalOrderId}&PayerID={id}
     * PayPal redirige ici après approbation du client.
     * On capture le paiement puis on redirige vers la page de succès.
     */
    @GetMapping("/paypal/capture")
    public void capturePaypal(
            @RequestParam("token") String paypalOrderId,
            @RequestParam(value = "PayerID", required = false) String payerId,
            HttpServletResponse response) throws IOException {

        log.info("PayPal capture — orderId={} payerId={}", paypalOrderId, payerId);

        try {
            paymentService.capturePaypalPayment(paypalOrderId);
            // Rediriger vers la page de succès frontend
            response.sendRedirect(frontendUrl + "/pay-success");
        } catch (Exception e) {
            log.error("Erreur capture PayPal : {}", e.getMessage());
            response.sendRedirect(frontendUrl + "/pay-error");
        }
    }

    /**
     * GET /api/payments/paypal/cancel
     * PayPal redirige ici si le client annule.
     */
    @GetMapping("/paypal/cancel")
    public void cancelPaypal(HttpServletResponse response) throws IOException {
        log.info("Paiement PayPal annulé par le client.");
        response.sendRedirect(frontendUrl + "/pay-cancelled");
    }

    /* ── Helper mapping ─────────────────────────────────── */

    private PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .gatewayRef(tx.getGatewayRef())
                .gateway(tx.getGateway())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .ipAddress(tx.getIpAddress())
                .paidAt(tx.getPaidAt())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
