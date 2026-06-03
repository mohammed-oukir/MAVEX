package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;
import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.model.enums.NotificationType;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;

import java.time.LocalDateTime;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailServiceImpl implements NotificationEmailService {

    private final JavaMailSender          mailSender;
    private final EmailTemplateRepository templateRepository;
    private final EmailLogRepository      emailLogRepository;
    private final OrderRepository         orderRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Transactional
    public SendEmailResponse sendPaymentEmail(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order introuvable id=" + orderId));

        if (order.getClient() == null || order.getClient().getEmail() == null) {
            throw new IllegalStateException("Le client n'a pas d'adresse email.");
        }

        if (!order.isTokenValid()) {
            order.generatePaymentToken();
            orderRepository.save(order);
        }

        EmailTemplate template = templateRepository
                .findByTypeAndActiveTrue(NotificationType.PAYMENT_INVOICE_WITH_AMOUNT)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun template email actif trouvé pour PAYMENT_INVOICE_WITH_AMOUNT. " +
                        "Veuillez exécuter le script SQL d'initialisation."));

        Map<String, String> variables = buildVariables(order);
        String htmlContent = template.resolveHtml(variables);
        String subject     = template.resolveSubject(variables);

        EmailLog emailLog = EmailLog.builder()
                .toEmail(order.getClient().getEmail())
                .subject(subject)
                .status(EmailStatus.PENDING)
                .emailTemplate(template)
                .order(order)
                .build();
        emailLogRepository.save(emailLog);

        try {
            sendHtmlEmail(order.getClient().getEmail(), subject, htmlContent);

            emailLog.markSent();
            emailLogRepository.save(emailLog);

            order.setEmailSentAt(LocalDateTime.now());
            if (order.getStatus() == OrderStatus.CREATED) {
                order.setStatus(OrderStatus.EMAIL_SENT);
            }
            orderRepository.save(order);

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

    @Override
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

    @Override
    @Transactional
    public BulkEmailResult sendBulkEmails(List<Long> orderIds) {
        int sent = 0, failed = 0;
        for (Long id : orderIds) {
            try {
                SendEmailResponse result = sendPaymentEmail(id);
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
    // ENVOI SMTP — méthode interne, non exposée dans l'interface
    // ---------------------------------------------------------------

    private void sendHtmlEmail(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    // ---------------------------------------------------------------
    // CONSTRUCTION DES VARIABLES — méthode interne
    // ---------------------------------------------------------------

    private Map<String, String> buildVariables(Order order) {
        Map<String, String> vars = new HashMap<>();

        vars.put("hawb",            safe(order.getHawb()));
        vars.put("goodsDescription",safe(order.getGoodsDescription()));
        vars.put("shipmentWeight",  order.getShipmentWeight() != null ? order.getShipmentWeight().toPlainString() : "—");
        vars.put("customsValue",    order.getCustomsValue()   != null ? order.getCustomsValue().toPlainString()   : "—");
        vars.put("dutyAmount",      order.getDutyAmount()     != null ? order.getDutyAmount().toPlainString()     : "—");
        vars.put("totalAmount",     order.getTotalAmount()    != null ? order.getTotalAmount().toPlainString()    : "—");
        vars.put("customsCurrency", safe(order.getCustomsCurrency()));

        if (order.getClient() != null) {
            vars.put("receiverName",    safe(order.getClient().getFullName()));
            vars.put("deliveryAddress", buildAddress(order));
        } else {
            vars.put("receiverName",    "—");
            vars.put("deliveryAddress", "—");
        }

        if (order.getShipment() != null && order.getShipment().getShipper() != null) {
            vars.put("shipperName", safe(order.getShipment().getShipper().getCompanyName()));
        } else {
            vars.put("shipperName", "—");
        }

        vars.put("paymentLink", frontendUrl + "/pay/" + order.getPaymentToken());

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
