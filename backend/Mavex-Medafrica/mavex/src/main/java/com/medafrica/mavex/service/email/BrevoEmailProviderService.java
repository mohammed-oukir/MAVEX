package com.medafrica.mavex.service.email;

import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.model.enums.EmailProviderType;
import com.medafrica.mavex.repository.EmailProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider Brevo via API REST (https://api.brevo.com/v3/smtp/email).
 * La cle API et les metadonnees expediteur sont lues depuis EmailProviderConfig en base,
 * jamais depuis application.properties.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailProviderService implements EmailProviderService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final EmailProviderConfigRepository configRepository;
    private final EncryptionService             encryptionService;
    private final RestClient                    restClient;

    @Override
    public EmailProviderType getType() {
        return EmailProviderType.BREVO;
    }

    @Override
    public String send(String toEmail, String subject, String htmlBody,
                      List<MultipartFile> attachments) throws Exception {

        EmailProviderConfig config = configRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Aucun provider email actif en base."));

        if (config.getBrevoApiKeyEncrypted() == null) {
            throw new IllegalStateException("La cle API Brevo n'est pas configuree.");
        }

        String apiKey = encryptionService.decrypt(config.getBrevoApiKeyEncrypted());

        Map<String, Object> payload = buildPayload(config, toEmail, subject, htmlBody, attachments);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(BREVO_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("api-key", apiKey)
                .body(payload)
                .retrieve()
                .body(Map.class);

        String messageId = (response != null) ? (String) response.get("messageId") : null;
        log.debug("Email Brevo envoye a {} — messageId={}", toEmail, messageId);
        return messageId;
    }

    private Map<String, Object> buildPayload(EmailProviderConfig config,
                                             String toEmail,
                                             String subject,
                                             String htmlBody,
                                             List<MultipartFile> attachments) throws Exception {
        Map<String, Object> payload = new HashMap<>();

        // Expediteur
        payload.put("sender", Map.of(
                "name",  config.getFromName(),
                "email", config.getFromEmail()
        ));

        // Destinataire
        payload.put("to", List.of(Map.of("email", toEmail)));

        payload.put("subject",     subject);
        payload.put("htmlContent", htmlBody);

        // Pieces jointes encodees en Base64
        if (attachments != null && !attachments.isEmpty()) {
            List<Map<String, String>> brevoAttachments = new ArrayList<>();
            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    brevoAttachments.add(Map.of(
                            "name",    file.getOriginalFilename() != null
                                           ? file.getOriginalFilename() : "attachment",
                            "content", Base64.getEncoder().encodeToString(file.getBytes())
                    ));
                }
            }
            if (!brevoAttachments.isEmpty()) {
                payload.put("attachment", brevoAttachments);
            }
        }

        return payload;
    }
}
