package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;
import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.model.enums.NotificationType;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.core.io.ClassPathResource;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailServiceImpl implements NotificationEmailService {

    private final EmailTemplateRepository templateRepository;
    private final EmailLogRepository      emailLogRepository;
    private final OrderRepository         orderRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name:MAVEX}")
    private String fromName;

    @Override
    @Transactional
    public SendEmailResponse sendPaymentEmail(Long orderId, MultipartFile[] attachments) {

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
            String messageId = sendHtmlEmail(order.getClient().getEmail(), subject, htmlContent, attachments);

            emailLog.markSent();
            emailLog.setSendgridMessageId(messageId);
            emailLogRepository.save(emailLog);

            order.setEmailSentAt(LocalDateTime.now());
            order.setEmailSentCount(order.getEmailSentCount() + 1);
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
    public Map<String, Object> sendAllPaymentEmails(Long shipmentId, MultipartFile[] attachments) {
        var orders = orderRepository.findByShipmentId(shipmentId);

        int sent   = 0;
        int failed = 0;

        for (Order order : orders) {
            try {
                SendEmailResponse result = sendPaymentEmail(order.getId(), attachments);
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
    public BulkEmailResult sendBulkEmails(List<Long> orderIds, MultipartFile[] attachments) {
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
    // ENVOI SENDGRID — retourne le message-id pour le tracking
    // ---------------------------------------------------------------

    private String sendHtmlEmail(String to, String subject, String html, MultipartFile[] attachments) throws Exception {
        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail, fromName));
        mail.setSubject(subject);

        com.sendgrid.helpers.mail.objects.Personalization personalization =
                new com.sendgrid.helpers.mail.objects.Personalization();
        personalization.addTo(new Email(to));
        mail.addPersonalization(personalization);

        mail.addContent(new Content("text/html", html));

        // Logo inline — cid:logo
        try {
            ClassPathResource logoResource = new ClassPathResource("static/medafrica-logo.jpeg");
            byte[] logoBytes = logoResource.getInputStream().readAllBytes();
            Attachments logoAttachment = new Attachments();
            logoAttachment.setContent(Base64.getEncoder().encodeToString(logoBytes));
            logoAttachment.setType("image/jpeg");
            logoAttachment.setFilename("medafrica-logo.jpeg");
            logoAttachment.setDisposition("inline");
            logoAttachment.setContentId("logo");
            mail.addAttachments(logoAttachment);
        } catch (Exception e) {
            log.warn("Logo introuvable, envoi sans logo : {}", e.getMessage());
        }

        if (attachments != null) {
            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    Attachments attachment = new Attachments();
                    attachment.setContent(Base64.getEncoder().encodeToString(file.getBytes()));
                    attachment.setType(file.getContentType());
                    attachment.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
                    attachment.setDisposition("attachment");
                    mail.addAttachments(attachment);
                }
            }
        }

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            throw new RuntimeException("SendGrid error " + response.getStatusCode() + " : " + response.getBody());
        }

        // SendGrid retourne l'ID dans le header X-Message-Id
        String messageId = response.getHeaders().get("X-Message-Id");
        log.debug("SendGrid message-id={}, status={}", messageId, response.getStatusCode());
        return messageId;
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
            vars.put("clientPhone",     safe(order.getClient().getPhone()));
        } else {
            vars.put("receiverName",    "—");
            vars.put("deliveryAddress", "—");
            vars.put("clientPhone",     "—");
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
