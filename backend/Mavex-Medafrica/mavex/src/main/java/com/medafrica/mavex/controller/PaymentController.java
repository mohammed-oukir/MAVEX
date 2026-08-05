package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.payment.PaymentCaptureResponse;
import com.medafrica.mavex.dto.payment.PaymentInitiateResponse;
import com.medafrica.mavex.dto.payment.PaymentTransactionResponse;
import com.medafrica.mavex.dto.payment.PaypalOrderResult;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.PaymentGatewayConfigRepository;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.service.payment.PaymentTransactionService;
import com.medafrica.mavex.service.payment.PaypalPaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository               orderRepository;
    private final PaymentGatewayConfigRepository gatewayConfigRepository;
    private final PaymentTransactionRepository   transactionRepository;
    private final PaypalPaymentService           paypalPaymentService;
    private final PaymentTransactionService      paymentTransactionService;

    // ─────────── POST /api/payments/pay/{token}/initiate (PUBLIC — pas d'auth) ───────────
    @PostMapping("/pay/{token}/initiate")
    public PaymentInitiateResponse initiate(@PathVariable String token, HttpServletRequest request) {
        Order order = orderRepository.findByPaymentToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Lien de paiement invalide ou expiré."));

        if (!order.isTokenValid()) {
            throw new IllegalStateException("Le lien de paiement a expiré.");
        }

        PaymentGatewayConfig activeGateway = gatewayConfigRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Aucun mode de paiement actif."));

        if (activeGateway.getType() != PaymentGatewayType.PAYPAL) {
            throw new IllegalStateException("Mode de paiement non supporté : " + activeGateway.getType());
        }

        PaypalOrderResult result = paypalPaymentService.createOrder(
                order.getId().toString(), order.getTotalAmount(), "USD");

        PaymentTransaction transaction = PaymentTransaction.builder()
                .gateway(PaymentGatewayType.PAYPAL)
                .gatewayRef(result.paypalOrderId())
                .amount(order.getTotalAmount())
                .currency("USD")
                .order(order)
                .ipAddress(request.getRemoteAddr())
                .build();
        transaction = transactionRepository.save(transaction);

        return PaymentInitiateResponse.builder()
                .redirectUrl(result.approveUrl())
                .transactionId(transaction.getId())
                .gateway("PAYPAL")
                .build();
    }

    // ─────────── POST /api/payments/capture/{paypalOrderId} (PUBLIC — pas d'auth) ───────────
    @PostMapping("/capture/{paypalOrderId}")
    public PaymentCaptureResponse capture(@PathVariable String paypalOrderId) {
        PaymentTransaction transaction = transactionRepository.findByGatewayRef(paypalOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction introuvable pour cette commande PayPal."));

        boolean captured = paypalPaymentService.captureOrder(paypalOrderId);
        Order order = transaction.getOrder();

        if (captured) {
            paymentTransactionService.markSuccess(transaction);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transactionRepository.save(transaction);
        }

        return PaymentCaptureResponse.builder()
                .success(captured)
                .orderStatus(order.getStatus().name())
                .build();
    }

    // ─────────── GET /api/payments — recherche paginée avec filtres (ADMIN uniquement) ───────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PaymentTransactionResponse> search(
            @RequestParam(required = false) String hawb,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) PaymentGatewayType gateway,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return paymentTransactionService.search(
                hawb, client, gateway, status, amountMin, amountMax, from, to, pageable);
    }

    // ─────────── GET /api/payments/total-collected (ADMIN uniquement) ───────────
    @GetMapping("/total-collected")
    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal getTotalCollected() {
        return paymentTransactionService.getTotalCollected();
    }
}
