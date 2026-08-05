package com.medafrica.mavex.controller;

import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.service.payment.PaymentTransactionService;
import com.medafrica.mavex.service.payment.PaypalPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Endpoint public (sans JWT) pour recevoir les notifications webhook PayPal.
 * PayPal appelle POST /webhooks/paypal avec la signature dans les headers Paypal-*.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/paypal")
@RequiredArgsConstructor
public class PaypalWebhookController {

    private final PaypalPaymentService         paypalPaymentService;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentTransactionService    paymentTransactionService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("Paypal-Auth-Algo") String authAlgo,
            @RequestHeader("Paypal-Cert-Url") String certUrl,
            @RequestHeader("Paypal-Transmission-Id") String transmissionId,
            @RequestHeader("Paypal-Transmission-Sig") String transmissionSig,
            @RequestHeader("Paypal-Transmission-Time") String transmissionTime,
            @RequestBody Map<String, Object> body) {

        String eventType = (String) body.get("event_type");
        log.info("Webhook PayPal reçu — event_type={}", eventType);

        boolean verified = paypalPaymentService.verifyWebhookSignature(
                authAlgo, certUrl, transmissionId, transmissionSig, transmissionTime, body);

        if (!verified) {
            log.warn("Webhook PayPal — signature invalide reçue");
            return ResponseEntity.status(401).build();
        }
        log.info("Webhook PayPal — signature vérifiée avec succès");

        if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
            String paypalOrderId = extractPaypalOrderId(body);
            if (paypalOrderId != null) {
                Optional<PaymentTransaction> opt = transactionRepository.findByGatewayRef(paypalOrderId);
                if (opt.isPresent()) {
                    paymentTransactionService.markSuccess(opt.get());
                    log.info("Webhook PayPal — transaction mise à jour avec succès pour orderId={}", paypalOrderId);
                } else {
                    log.warn("Webhook PayPal — aucune transaction pour orderId={}", paypalOrderId);
                }
            } else {
                log.warn("Webhook PayPal — order_id introuvable dans le payload event_type={}", eventType);
            }
        } else {
            log.debug("Webhook PayPal — événement non géré : {}", eventType);
        }

        return ResponseEntity.ok().build();
    }

    // ---------------------------------------------------------------
    // NAVIGATION SURE DANS LA MAP IMBRIQUEE
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        return (obj instanceof Map<?, ?> map) ? (Map<String, Object>) map : null;
    }

    private String extractPaypalOrderId(Map<String, Object> body) {
        Map<String, Object> resource = asMap(body.get("resource"));
        if (resource == null) return null;

        Map<String, Object> supplementaryData = asMap(resource.get("supplementary_data"));
        if (supplementaryData == null) return null;

        Map<String, Object> relatedIds = asMap(supplementaryData.get("related_ids"));
        if (relatedIds == null) return null;

        Object orderId = relatedIds.get("order_id");
        return (orderId instanceof String s) ? s : null;
    }
}
