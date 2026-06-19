package com.medafrica.mavex.dto.email;

import com.medafrica.mavex.model.enums.EmailProviderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailProviderConfigRequest {

    @NotNull(message = "Le type de provider est obligatoire")
    private EmailProviderType providerType;

    @NotBlank(message = "L'adresse expediteur est obligatoire")
    @Email(message = "L'adresse expediteur doit etre une adresse email valide")
    private String fromEmail;

    @NotBlank(message = "Le nom expediteur est obligatoire")
    private String fromName;

    // --- Champs SMTP (optionnels — null ignore si providerType = BREVO) ---

    private String  smtpHost;
    private Integer smtpPort;
    private String  smtpUsername;
    private Boolean smtpSslEnabled;
    private Boolean smtpTlsEnabled;

    /** Mot de passe SMTP en clair — null ou vide = conserver la valeur existante en base */
    private String smtpPassword;

    // --- Champs Brevo (optionnels — null ignore si providerType = SMTP) ---

    /** Cle API Brevo en clair — null ou vide = conserver la valeur existante en base */
    private String brevoApiKey;

    /** Token webhook Brevo en clair — null ou vide = conserver la valeur existante en base */
    private String brevoWebhookToken;
}
