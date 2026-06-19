package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.NotificationType;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.service.email.EmailProviderResolver;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import java.util.List;
import java.util.Map;
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

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // ---------------------------------------------------------------
    // API PUBLIQUE
    // ---------------------------------------------------------------

    @Override
    public SendEmailResponse sendPaymentEmail(Long orderId, MultipartFile[] attachments) {

        // 1. Lecture + préparation BDD (transaction propre dans EmailPersistenceService)
        Order order = persistence.loadAndPrepareOrder(orderId);
            
        EmailTemplate template = templateRepository
                .findByTypeAndActiveTrue(NotificationType.PAYMENT_INVOICE_WITH_AMOUNT)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun template email actif trouvé pour PAYMENT_INVOICE_WITH_AMOUNT. " +
                        "Veuillez exécuter le script SQL d'initialisation."));

        Map<String, String> variables = buildVariables(order);
        String htmlContent = template.resolveHtml(variables);
        String subject     = template.resolveSubject(variables);
        String toEmail     = order.getClient().getEmail();

        // 2. Log PENDING en BDD — commit immédiat, avant d'envoyer le SMTP
        Long emailLogId = persistence.createPendingLog(toEmail, subject, template, order.getId());

        try {
            // 3. Envoi via provider actif — hors transaction BDD intentionnellement
            String messageId = providerResolver.resolve().send(
                toEmail, subject, htmlContent,
                attachments != null ? List.of(attachments) : List.of()
            );

            // 4. Mise à jour BDD → SENT (recharge les entités fraîches par ID)
            persistence.markSuccess(emailLogId, order.getId(), messageId);

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
    public BulkEmailResult sendAllPaymentEmails(Long shipmentId, MultipartFile[] attachments) {
        List<Long> orderIds = orderRepository.findByShipmentId(shipmentId)
                .stream().map(Order::getId).toList();
        return executeBulk(orderIds, attachments);
    }

    @Override
    public BulkEmailResult sendBulkEmails(List<Long> orderIds, MultipartFile[] attachments) {
        return executeBulk(orderIds, attachments);
    }

    // ---------------------------------------------------------------
    // LOGIQUE BULK COMMUNE (évite la duplication)
    // ---------------------------------------------------------------

    private BulkEmailResult executeBulk(List<Long> orderIds, MultipartFile[] attachments) {
        int sent = 0, failed = 0;
        for (Long id : orderIds) {
            try {
                SendEmailResponse result = sendPaymentEmail(id, attachments);
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
