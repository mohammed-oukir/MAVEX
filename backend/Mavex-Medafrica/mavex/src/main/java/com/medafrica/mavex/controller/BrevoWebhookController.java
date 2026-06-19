package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.BrevoWebhookEvent;
import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.repository.EmailProviderConfigRepository;
import com.medafrica.mavex.service.email.BrevoWebhookService;
import com.medafrica.mavex.service.email.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Endpoint public (sans JWT) pour recevoir les notifications webhook Brevo.
 * Brevo appelle POST /webhooks/brevo/{token} avec un tableau d'événements.
 * Le token dans l'URL est comparé au brevoWebhookToken déchiffré en base.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/brevo")
@RequiredArgsConstructor
public class BrevoWebhookController {

    private final EmailProviderConfigRepository configRepository;
    private final EncryptionService             encryptionService;
    private final BrevoWebhookService           webhookService;

    @PostMapping("/{token}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String token,
            @RequestBody BrevoWebhookEvent event) {

        if (!isTokenValid(token)) {
            log.warn("Webhook Brevo — token invalide reçu");
            return ResponseEntity.status(401).build();
        }

        try {
            webhookService.processEvent(event);
        } catch (Exception e) {
            log.error("Webhook Brevo — erreur traitement event={} : {}",
                      event.getEvent(), e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    private boolean isTokenValid(String token) {
        Optional<EmailProviderConfig> opt = configRepository.findByActiveTrue();
        if (opt.isEmpty()) return false;

        EmailProviderConfig config = opt.get();
        if (config.getBrevoWebhookTokenEncrypted() == null) return false;

        try {
            String expected = encryptionService.decrypt(config.getBrevoWebhookTokenEncrypted());
            return expected.equals(token);
        } catch (Exception e) {
            log.error("Webhook Brevo — erreur déchiffrement token : {}", e.getMessage());
            return false;
        }
    }
}
