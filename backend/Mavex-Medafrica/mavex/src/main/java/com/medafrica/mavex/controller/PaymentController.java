package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.payment.ExportSelectionRequest;
import com.medafrica.mavex.dto.payment.PaymentCaptureResponse;
import com.medafrica.mavex.dto.payment.PaymentInitiateResponse;
import com.medafrica.mavex.dto.payment.PaymentTransactionResponse;
import com.medafrica.mavex.dto.payment.PaypalOrderResult;
import com.medafrica.mavex.dto.payment.ReceiptSendResponse;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.PaymentGatewayConfigRepository;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.service.export.TableExportService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository               orderRepository;
    private final PaymentGatewayConfigRepository gatewayConfigRepository;
    private final PaymentTransactionRepository   transactionRepository;
    private final PaypalPaymentService           paypalPaymentService;
    private final PaymentTransactionService      paymentTransactionService;
    private final TableExportService             exportService;

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

        PaymentTransaction transaction = transactionRepository
                .findByOrderIdAndStatus(order.getId(), PaymentStatus.INITIATED)
                .orElseGet(() -> PaymentTransaction.builder()
                        .gateway(PaymentGatewayType.PAYPAL)
                        .order(order)
                        .ipAddress(request.getRemoteAddr())
                        .build());

        transaction.setGatewayRef(result.paypalOrderId());
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrency("USD");
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

    // ─────────── GET /api/payments — recherche paginée avec filtres ───────────
    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('PAYMENTS','VIEW')")
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

    // ─────────── GET /api/payments/total-collected ───────────
    @GetMapping("/total-collected")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('PAYMENTS','VIEW')")
    public BigDecimal getTotalCollected() {
        return paymentTransactionService.getTotalCollected();
    }

    // ─────────── POST /api/payments/{transactionId}/send-receipt ───────────
    @PostMapping("/{transactionId}/send-receipt")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('PAYMENTS','SEND_RECEIPT')")
    public ReceiptSendResponse sendReceipt(@PathVariable Long transactionId) {
        paymentTransactionService.sendManualReceipt(transactionId);
        return ReceiptSendResponse.builder()
                .success(true)
                .message("Reçu envoyé avec succès.")
                .build();
    }

    // ─────────── GET /api/payments/export/pdf — export PDF des transactions filtrées ───────────
    @GetMapping("/export/pdf")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('PAYMENTS','EXPORT')")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String hawb,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) PaymentGatewayType gateway,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<PaymentTransaction> transactions = paymentTransactionService.searchAll(
                hawb, client, gateway, status, amountMin, amountMax, from, to);
        List<List<String>> rows = rowsFromTransactions(transactions);

        byte[] bytes = exportService.generatePdf("Liste des transactions", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ─────────── GET /api/payments/export/excel — export Excel des transactions filtrées ───────────
    @GetMapping("/export/excel")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('PAYMENTS','EXPORT')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String hawb,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) PaymentGatewayType gateway,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<PaymentTransaction> transactions = paymentTransactionService.searchAll(
                hawb, client, gateway, status, amountMin, amountMax, from, to);
        List<List<String>> rows = rowsFromTransactions(transactions);

        byte[] bytes = exportService.generateExcel("Transactions", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ─────────── POST /api/payments/export/excel/selection — export Excel d'une sélection d'ids ───────────
    @PostMapping("/export/excel/selection")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('PAYMENTS','EXPORT')")
    public ResponseEntity<byte[]> exportExcelSelection(@RequestBody ExportSelectionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("Veuillez sélectionner au moins une transaction.");
        }

        List<PaymentTransaction> transactions = paymentTransactionService.findAllByIds(request.getIds());
        List<List<String>> rows = rowsFromTransactions(transactions);

        byte[] bytes = exportService.generateExcel("Transactions (sélection)", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions_selection.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static final List<String> EXPORT_HEADERS =
            List.of("HAWB", "Client", "Mode", "Montant", "Statut", "Date");

    private static final DateTimeFormatter EXPORT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<List<String>> rowsFromTransactions(List<PaymentTransaction> transactions) {
        List<List<String>> rows = new ArrayList<>();
        for (PaymentTransaction t : transactions) {
            String hawb = (t.getOrder() != null && t.getOrder().getHawb() != null)
                    ? t.getOrder().getHawb() : "";
            String clientName = (t.getOrder() != null && t.getOrder().getClient() != null
                    && t.getOrder().getClient().getFullName() != null)
                    ? t.getOrder().getClient().getFullName() : "";
            String gatewayName = t.getGateway() != null ? t.getGateway().name() : "";
            String currency = t.getCurrency() != null ? t.getCurrency() : "";
            String amountVal = t.getAmount() != null
                    ? t.getAmount().toString() + " " + currency
                    : "";
            String statusName = t.getStatus() != null ? t.getStatus().name() : "";
            String dateVal = t.getCreatedAt() != null
                    ? t.getCreatedAt().toLocalDate().format(EXPORT_DATE_FMT) : "";

            rows.add(List.of(hawb, clientName, gatewayName, amountVal, statusName, dateVal));
        }
        return rows;
    }
}
