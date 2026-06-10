package com.medafrica.mavex.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class SendgridWebhookController {

    private final EmailLogRepository emailLogRepository;
    private final OrderRepository    orderRepository;

    /**
     * SendGrid poste ici les événements email (open, click, bounce...).
     * L'endpoint doit être public (pas de JWT requis).
     */
    @PostMapping("/sendgrid")
    public ResponseEntity<Void> handleSendgridEvents(@RequestBody JsonNode events) {

        if (!events.isArray()) {
            return ResponseEntity.badRequest().build();
        }

        for (JsonNode event : events) {
            String eventType = event.path("event").asText();
            String messageId = event.path("sg_message_id").asText();

            // SendGrid ajoute parfois un suffixe après un point — on prend seulement la partie principale
            if (messageId.contains(".")) {
                messageId = messageId.substring(0, messageId.indexOf("."));
            }

            log.debug("Webhook SendGrid reçu — event={}, messageId={}", eventType, messageId);

            if ("open".equals(eventType) && !messageId.isBlank()) {
                long timestamp = event.path("timestamp").asLong();
                LocalDateTime openedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()
                );

                Optional<EmailLog> logOpt = emailLogRepository.findBySendgridMessageId(messageId);
                if (logOpt.isPresent()) {
                    EmailLog emailLog = logOpt.get();
                    if (emailLog.getOpenedAt() == null) {
                        emailLog.setOpenedAt(openedAt);
                        emailLogRepository.save(emailLog);

                        // EMAIL_SENT → PENDING_PAYMENT automatiquement quand le client ouvre l'email
                        Order order = emailLog.getOrder();
                        if (order != null && order.getStatus() == OrderStatus.EMAIL_SENT) {
                            order.setStatus(OrderStatus.PENDING_PAYMENT);
                            orderRepository.save(order);
                            log.info("Statut auto EMAIL_SENT → PENDING_PAYMENT — order={}", order.getHawb());
                        }

                        log.info("Email ouvert — order={}, messageId={}, at={}",
                                order != null ? order.getHawb() : "N/A",
                                messageId, openedAt);
                    }
                } else {
                    log.warn("Webhook open reçu mais messageId introuvable en base : {}", messageId);
                }
            }
        }

        return ResponseEntity.ok().build();
    }
}
