package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.finance.ExchangeRate;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.ManualPaymentConfig;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.repository.ExchangeRateRepository;
import com.medafrica.mavex.repository.ManualPaymentConfigRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.PaymentGatewayConfigRepository;
import com.medafrica.mavex.service.email.EmailProviderResolver;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import com.medafrica.mavex.service.payment.ByteArrayMultipartFile;
import com.medafrica.mavex.service.payment.PaymentReceiptService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrateur : charge les données, envoie le SMTP, délègue les écritures BDD
 * à EmailPersistenceService (bean séparé → @Transactional fonctionne réellement).
 *
 * Aucun @Transactional ici : l'envoi SMTP ne doit pas être dans une transaction BDD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailServiceImpl implements NotificationEmailService {

    private final EmailTemplateRepository  templateRepository;
    private final OrderRepository          orderRepository;
    private final EmailProviderResolver    providerResolver;
    private final EmailPersistenceService  persistence;
    private final ExchangeRateRepository   exchangeRateRepository;
    private final PaymentGatewayConfigRepository gatewayConfigRepository;
    private final ManualPaymentConfigRepository  manualPaymentConfigRepository;
    private final PaymentReceiptService    paymentReceiptService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // ---------------------------------------------------------------
    // API PUBLIQUE
    // ---------------------------------------------------------------

    @Override
    public SendEmailResponse sendPaymentEmail(Long orderId) {
        Optional<ExchangeRate> activeRate = exchangeRateRepository
            .findByIsActiveTrueAndFromCurrencyAndToCurrency("USD", "MAD");
        if (activeRate.isEmpty()) {
            log.warn("Aucun taux actif USD→MAD trouvé — lockedExchangeRate ne sera pas figé pour Order id={}", orderId);
        }
        return doSendPaymentEmail(orderId, activeRate);
    }

    private SendEmailResponse doSendPaymentEmail(Long orderId, Optional<ExchangeRate> activeRate) {

        // 1. Lecture + préparation BDD (transaction propre dans EmailPersistenceService)
        Order order = persistence.loadAndPrepareOrder(orderId);

        String templateType = resolveTemplateType();

        EmailTemplate template = templateRepository
                .findByType_NameAndActiveTrue(templateType)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun template email actif trouvé pour " + templateType + ". " +
                        "Veuillez exécuter le script SQL d'initialisation."));

        Map<String, String> variables = buildVariables(order);
        String htmlContent = template.resolveHtml(variables);
        String subject     = template.resolveSubject(variables);
        String toEmail     = order.getClient().getEmail();

        // 2. Log PENDING en BDD — commit immédiat, avant d'envoyer le SMTP
        Long emailLogId = persistence.createPendingLog(toEmail, subject, template, order.getId());

        try {
            List<MultipartFile> attachments = resolveAttachments();

            // 3. Envoi via provider actif — hors transaction BDD intentionnellement
            String messageId = providerResolver.resolve().send(
                toEmail, subject, htmlContent,
                attachments
            );

            // 4. Mise à jour BDD → SENT (recharge les entités fraîches par ID)
            persistence.markSuccess(emailLogId, order.getId(), messageId, activeRate);

            log.info("Email envoyé avec succès → {} (Order HAWB={})", toEmail, order.getHawb());

            return SendEmailResponse.builder()
                    .success(true)
                    .message("Email envoyé avec succès")
                    .toEmail(toEmail)
                    .hawb(order.getHawb())
                    .orderStatus(order.getStatus().name())
                    .build();

        } catch (Exception e) {
            // 5. Mise à jour BDD → FAILED (recharge le log frais par ID)
            persistence.markFailed(emailLogId, e.getMessage());

            log.error("Échec envoi email → {} : {}", toEmail, e.getMessage());

            return SendEmailResponse.builder()
                    .success(false)
                    .message("Échec envoi email : " + e.getMessage())
                    .toEmail(toEmail)
                    .hawb(order.getHawb())
                    .orderStatus(order.getStatus().name())
                    .build();
        }
    }

    @Override
    public BulkEmailResult sendAllPaymentEmails(Long shipmentId) {
        List<Long> orderIds = orderRepository.findByShipmentId(shipmentId)
                .stream().map(Order::getId).toList();
        return executeBulk(orderIds);
    }

    @Override
    public BulkEmailResult sendBulkEmails(List<Long> orderIds) {
        return executeBulk(orderIds);
    }

    // ---------------------------------------------------------------
    // CONFIRMATION DE PAIEMENT + RECU PDF (best-effort — ne doit jamais
    // faire échouer le flux appelant, ex: markSuccess() d'un paiement)
    // ---------------------------------------------------------------

    private static final String PAYMENT_CONFIRMED_TEMPLATE = "PAYMENT_CONFIRMED";

    @Override
    public void sendPaymentConfirmationEmail(Order order, PaymentTransaction transaction) {
        try {
            Optional<EmailTemplate> templateOpt = templateRepository
                    .findByType_NameAndActiveTrue(PAYMENT_CONFIRMED_TEMPLATE);

            if (templateOpt.isEmpty()) {
                log.warn("Aucun template email actif trouvé pour {} — confirmation de paiement non envoyée (Order id={})",
                        PAYMENT_CONFIRMED_TEMPLATE, order.getId());
                return;
            }
            EmailTemplate template = templateOpt.get();

            Map<String, String> variables = buildVariables(order);
            String htmlContent = template.resolveHtml(variables);
            String subject     = template.resolveSubject(variables);
            String toEmail     = order.getClient().getEmail();

            byte[] receiptPdf;
            try {
                receiptPdf = paymentReceiptService.generateReceipt(order, transaction);
            } catch (Exception e) {
                log.error("Échec génération du reçu PDF — confirmation de paiement non envoyée (Order id={}) : {}",
                        order.getId(), e.getMessage(), e);
                return;
            }

            Long emailLogId = persistence.createPendingLog(toEmail, subject, template, order.getId());

            try {
                MultipartFile receiptFile = new ByteArrayMultipartFile(
                        "receipt",
                        "receipt-" + order.getHawb() + ".pdf",
                        "application/pdf",
                        receiptPdf
                );

                String messageId = providerResolver.resolve().send(
                        toEmail, subject, htmlContent, List.of(receiptFile)
                );

                persistence.markLogSuccess(emailLogId, messageId);

                log.info("Email de confirmation de paiement envoyé → {} (Order HAWB={})", toEmail, order.getHawb());

            } catch (Exception e) {
                persistence.markLogFailed(emailLogId, e.getMessage());
                log.error("Échec envoi email de confirmation de paiement → {} : {}", toEmail, e.getMessage());
            }

        } catch (Exception e) {
            // Filet de sécurité ultime : cette méthode ne doit jamais remonter d'exception
            // à l'appelant (markSuccess() d'un paiement réel ne doit jamais échouer à cause d'un email).
            log.error("Erreur inattendue lors de l'envoi de la confirmation de paiement (Order id={}) : {}",
                    order.getId(), e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // LOGIQUE BULK COMMUNE (évite la duplication)
    // ---------------------------------------------------------------

    private BulkEmailResult executeBulk(List<Long> orderIds) {
        Optional<ExchangeRate> activeRate = exchangeRateRepository
            .findByIsActiveTrueAndFromCurrencyAndToCurrency("USD", "MAD");
        if (activeRate.isEmpty()) {
            log.warn("Aucun taux actif USD→MAD trouvé — lockedExchangeRate ne sera pas figé pour ce bulk de {} orders", orderIds.size());
        }
        int sent = 0, failed = 0;
        for (Long id : orderIds) {
            try {
                SendEmailResponse result = doSendPaymentEmail(id, activeRate);
                if (result.isSuccess()) sent++;
                else                    failed++;
            } catch (Exception e) {
                log.error("Bulk email — erreur order {} : {}", id, e.getMessage());
                failed++;
            }
        }
        return BulkEmailResult.builder()
                .total(orderIds.size())
                .sent(sent)
                .failed(failed)
                .build();
    }

    // ---------------------------------------------------------------
    // SELECTION DYNAMIQUE DU TEMPLATE (selon le mode de paiement actif)
    // ---------------------------------------------------------------

    private String resolveTemplateType() {
        Optional<PaymentGatewayType> activeType = gatewayConfigRepository.findByActiveTrue()
                .map(config -> config.getType());

        if (activeType.isEmpty()) {
            log.warn("Aucun mode de paiement actif — utilisation du template PAYMENT_INVOICE_MANUELLE par défaut.");
            return "PAYMENT_INVOICE_MANUELLE";
        }

        return activeType.get() == PaymentGatewayType.MANUEL
                ? "PAYMENT_INVOICE_MANUELLE"
                : "PAYMENT_INVOICE_ONLINE";
    }

    // ---------------------------------------------------------------
    // PIECE JOINTE AUTOMATIQUE (RIB si mode MANUEL actif)
    // ---------------------------------------------------------------

    private List<MultipartFile> resolveAttachments() {
        PaymentGatewayType activeType = gatewayConfigRepository.findByActiveTrue()
                .map(config -> config.getType())
                .orElse(null);

        if (activeType != PaymentGatewayType.MANUEL) {
            return List.of();
        }

        List<ManualPaymentConfig> configs = manualPaymentConfigRepository.findAll();
        if (configs.isEmpty()) {
            log.warn("Mode MANUEL actif mais aucun RIB configure — email envoye sans piece jointe.");
            return List.of();
        }

        ManualPaymentConfig ribConfig = configs.get(0);
        String filePath = ribConfig.getRibFilePath();
        if (filePath == null || filePath.isBlank()) {
            log.warn("Mode MANUEL actif mais chemin du RIB vide — email envoye sans piece jointe.");
            return List.of();
        }

        try {
            byte[] bytes = Files.readAllBytes(Path.of(filePath));
            MultipartFile ribFile = new ByteArrayMultipartFile(
                    "rib",
                    ribConfig.getRibFileName(),
                    "application/pdf",
                    bytes
            );
            List<MultipartFile> attachments = new ArrayList<>();
            attachments.add(ribFile);
            return attachments;
        } catch (IOException e) {
            log.warn("Impossible de lire le fichier RIB sur disque ({}) — email envoye sans piece jointe.", filePath, e);
            return List.of();
        }
    }

    // ---------------------------------------------------------------
    // CONSTRUCTION DES VARIABLES TEMPLATE
    // ---------------------------------------------------------------

    private Map<String, String> buildVariables(Order order) {
        var client   = order.getClient();
        var shipment = order.getShipment();

        return Map.ofEntries(
            Map.entry("hawb",             safe(order.getHawb())),
            Map.entry("goodsDescription", safe(order.getGoodsDescription())),
            Map.entry("shipmentWeight",   decimal(order.getShipmentWeight())),
            Map.entry("customsValue",     decimal(order.getCustomsValue())),
            Map.entry("dutyAmount",       decimal(order.getDutyAmount())),
            Map.entry("totalAmount",      decimal(order.getTotalAmount())),
            Map.entry("customsCurrency",  safe(order.getCustomsCurrency())),
            Map.entry("receiverName",     client != null ? safe(client.getFullName())  : "—"),
            Map.entry("deliveryAddress",  client != null ? buildAddress(order)         : "—"),
            Map.entry("clientPhone",      client != null ? safe(client.getPhone())     : "—"),
            Map.entry("shipperName",      shipment != null && shipment.getShipper() != null
                                              ? safe(shipment.getShipper().getCompanyName()) : "—"),
            Map.entry("paymentLink",      frontendUrl + "/pay/" + order.getPaymentToken())
        );
    }

    private String buildAddress(Order order) {
        var c  = order.getClient();
        var sb = new StringBuilder();
        if (c.getAddress() != null) sb.append(c.getAddress()).append(", ");
        if (c.getCity()    != null) sb.append(c.getCity()).append(", ");
        if (c.getState()   != null) sb.append(c.getState()).append(" ");
        if (c.getZipCode() != null) sb.append(c.getZipCode());
        return sb.toString().trim().replaceAll(",\\s*$", "");
    }

    private String safe(String val) {
        return val != null ? val : "—";
    }

    private String decimal(java.math.BigDecimal val) {
        return val != null ? val.toPlainString() : "—";
    }
}
