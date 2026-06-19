package com.medafrica.mavex.service.email;

import com.medafrica.mavex.model.enums.EmailProviderType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Contrat unique pour tous les providers d'envoi email.
 * Chaque implementation (Brevo, SMTP) est selectionnee par EmailProviderResolver
 * selon le provider actif en base — l'orchestrateur n'en sait rien.
 */
public interface EmailProviderService {

    /**
     * Envoie un email HTML avec pieces jointes optionnelles.
     * L'implementation est responsable de dechiffrer ses propres secrets
     * et de construire la connexion (SMTP) ou l'appel HTTP (Brevo).
     *
     * @return messageId provider (ex. Brevo), ou null si le provider n'en expose pas (SMTP)
     * @throws Exception tout echec technique remonte a l'orchestrateur qui loggue l'erreur
     */
    String send(String toEmail,
                String subject,
                String htmlBody,
                List<MultipartFile> attachments) throws Exception;

    /** Identifiant du provider — utilise par EmailProviderResolver pour la selection */
    EmailProviderType getType();
}
