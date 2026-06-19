package com.medafrica.mavex.service.email;

import com.medafrica.mavex.dto.email.BrevoWebhookEvent;
import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoWebhookService {

    private final EmailLogRepository emailLogRepository;

    @Transactional
    public void processEvent(BrevoWebhookEvent event) {
        String messageId = event.getMessageId();
        if (messageId == null || messageId.isBlank()) {
            log.warn("Webhook Brevo reçu sans messageId — ignoré (event={})", event.getEvent());
            return;
        }

        Optional<EmailLog> opt = emailLogRepository.findByMessageId(messageId);
        if (opt.isEmpty()) {
            log.warn("Webhook Brevo — aucun EmailLog pour messageId={} event={}",
                     messageId, event.getEvent());
            return;
        }

        EmailLog emailLog = opt.get();
        LocalDateTime now = LocalDateTime.now();

        switch (event.getEvent()) {
            case "delivered"   -> emailLog.setDeliveredAt(now);
            case "opened"      -> { if (emailLog.getOpenedAt()  == null) emailLog.setOpenedAt(now);  }
            case "clicked"     -> { if (emailLog.getClickedAt() == null) emailLog.setClickedAt(now); }
            case "hard_bounce",
                 "soft_bounce" -> {
                    emailLog.setBouncedAt(now);
                    emailLog.setBounceReason(event.getEvent());
                 }
            default -> log.debug("Webhook Brevo — événement non géré : {}", event.getEvent());
        }

        emailLogRepository.save(emailLog);
        log.debug("Webhook Brevo traité : event={} messageId={}", event.getEvent(), messageId);
    }
}
