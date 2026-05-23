package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.model.enums.NotificationType;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.repository.OrderRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final JavaMailSender          mailSender;
    private final EmailTemplateRepository templateRepository;
    private final EmailLogRepository      emailLogRepository;
    private final OrderRepository         orderRepository;

    @Value("${app.base-url:http://localhost:1111}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ---------------------------------------------------------------
    // ENVOYER EMAIL DE PAIEMENT POUR UN ORDER
    // ---------------------------------------------------------------

    @Transactional
    public SendEmailResponse sendPaymentEmail(Long orderId) {

        // 1. Récupérer l'order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order introuvable id=" + orderId));

        // 2. Vérifier que le client a un email
        if (order.getClient() == null || order.getClient().getEmail() == null) {
            throw new IllegalStateException("Le client n'a pas d'adresse email.");
        }

        // 3. Générer le payment token si pas encore fait
        if (!order.isTokenValid()) {
            order.generatePaymentToken();
            orderRepository.save(order);
        }

        // 4. Chercher le template actif
        EmailTemplate template = templateRepository
                .findByTypeAndActiveTrue(NotificationType.PAYMENT_INVOICE_WITH_AMOUNT)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun template email actif trouvé pour PAYMENT_INVOICE_WITH_AMOUNT. " +
                        "Veuillez exécuter le script SQL d'initialisation."));

        // 5. Construire les variables
        Map<String, String> variables = buildVariables(order);

        // 6. Résoudre le HTML et le sujet
        String htmlContent = template.resolveHtml(variables);
        String subject     = template.resolveSubject(variables);

        // 7. Créer le EmailLog (PENDING)
        EmailLog emailLog = EmailLog.builder()
                .toEmail(order.getClient().getEmail())
                .subject(subject)
                .status(EmailStatus.PENDING)
                .emailTemplate(template)
                .order(order)
                .build();
        emailLogRepository.save(emailLog);

        // 8. Envoyer l'email
        try {
            sendHtmlEmail(order.getClient().getEmail(), subject, htmlContent);

            // 9. Marquer SENT
            emailLog.markSent();
            emailLogRepository.save(emailLog);

            // 10. Changer statut order → EMAIL_SENT
            if (order.getStatus() == OrderStatus.CREATED) {
                order.setStatus(OrderStatus.EMAIL_SENT);
                orderRepository.save(order);
            }

            log.info("Email envoyé avec succès → {} (Order HAWB={})",
                    order.getClient().getEmail(), order.getHawb());

            return SendEmailResponse.builder()
                    .success(true)
                    .message("Email envoyé avec succès")
                    .toEmail(order.getClient().getEmail())
                    .hawb(order.getHawb())
                    .orderStatus(order.getStatus().name())
                    .build();

        } catch (Exception e) {
            // 11. Marquer FAILED
            emailLog.markFailed(e.getMessage());
            emailLogRepository.save(emailLog);

            log.error("Échec envoi email → {} : {}", order.getClient().getEmail(), e.getMessage());

            return SendEmailResponse.builder()
                    .success(false)
                    .message("Échec envoi email : " + e.getMessage())
                    .toEmail(order.getClient().getEmail())
                    .hawb(order.getHawb())
                    .orderStatus(order.getStatus().name())
                    .build();
        }
    }

    // ---------------------------------------------------------------
    // ENVOYER EMAILS POUR TOUS LES ORDERS D'UN SHIPMENT
    // ---------------------------------------------------------------

    @Transactional
    public Map<String, Object> sendAllPaymentEmails(Long shipmentId) {
        var orders = orderRepository.findByShipmentId(shipmentId);

        int sent   = 0;
        int failed = 0;

        for (Order order : orders) {
            try {
                SendEmailResponse result = sendPaymentEmail(order.getId());
                if (result.isSuccess()) sent++;
                else failed++;
            } catch (Exception e) {
                log.error("Erreur order {} : {}", order.getHawb(), e.getMessage());
                failed++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total",  orders.size());
        summary.put("sent",   sent);
        summary.put("failed", failed);
        return summary;
    }

    // ---------------------------------------------------------------
    // ENVOI SMTP
    // ---------------------------------------------------------------

    private void sendHtmlEmail(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true); // true = HTML
        mailSender.send(message);
    }

    // ---------------------------------------------------------------
    // CONSTRUCTION DES VARIABLES POUR LE TEMPLATE
    // ---------------------------------------------------------------

    private Map<String, String> buildVariables(Order order) {
        Map<String, String> vars = new HashMap<>();

        // Order
        vars.put("hawb",            safe(order.getHawb()));
        vars.put("goodsDescription",safe(order.getGoodsDescription()));
        vars.put("shipmentWeight",  order.getShipmentWeight() != null ? order.getShipmentWeight().toPlainString() : "—");
        vars.put("customsValue",    order.getCustomsValue()   != null ? order.getCustomsValue().toPlainString()   : "—");
        vars.put("dutyAmount",      order.getDutyAmount()     != null ? order.getDutyAmount().toPlainString()     : "—");
        vars.put("totalAmount",     order.getTotalAmount()    != null ? order.getTotalAmount().toPlainString()    : "—");
        vars.put("customsCurrency", safe(order.getCustomsCurrency()));

        // Client
        if (order.getClient() != null) {
            vars.put("receiverName", safe(order.getClient().getFullName()));
            vars.put("deliveryAddress", buildAddress(order));
        } else {
            vars.put("receiverName",    "—");
            vars.put("deliveryAddress", "—");
        }

        // Shipper
        if (order.getShipment() != null && order.getShipment().getShipper() != null) {
            vars.put("shipperName", safe(order.getShipment().getShipper().getCompanyName()));
        } else {
            vars.put("shipperName", "—");
        }

        // Lien de paiement
        vars.put("paymentLink", baseUrl + "/pay/" + order.getPaymentToken());

        return vars;
    }

    private String buildAddress(Order order) {
        var c = order.getClient();
        StringBuilder sb = new StringBuilder();
        if (c.getAddress() != null) sb.append(c.getAddress()).append(", ");
        if (c.getCity()    != null) sb.append(c.getCity()).append(", ");
        if (c.getState()   != null) sb.append(c.getState()).append(" ");
        if (c.getZipCode() != null) sb.append(c.getZipCode());
        return sb.toString().trim().replaceAll(",\\s*$", "");
    }

    private String safe(String val) {
        return val != null ? val : "—";
    }
}