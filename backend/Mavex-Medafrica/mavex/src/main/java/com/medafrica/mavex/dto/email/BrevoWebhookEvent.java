package com.medafrica.mavex.dto.email;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Corps d'une notification webhook Brevo.
 * Brevo envoie un tableau de ces objets en POST sur l'URL configurée.
 * Voir : https://developers.brevo.com/docs/transactional-webhooks
 */
@Getter @Setter @NoArgsConstructor
public class BrevoWebhookEvent {

    /** Type d'événement : "delivered", "opened", "clicked", "hard_bounce", "soft_bounce" */
    private String event;

    /** Adresse du destinataire */
    private String email;

    /** Identifiant unique du message — champ JSON "message-id" (avec tiret) */
    @JsonProperty("message-id")
    private String messageId;

    /** Date ISO 8601 de l'événement envoyée par Brevo */
    private String date;
}
