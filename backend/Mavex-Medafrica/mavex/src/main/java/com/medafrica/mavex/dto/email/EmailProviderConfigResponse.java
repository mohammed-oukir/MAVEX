package com.medafrica.mavex.dto.email;

import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.model.enums.EmailProviderType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailProviderConfigResponse {

    private Long              id;
    private EmailProviderType providerType;
    private boolean           active;

    // Champs communs
    private String fromEmail;
    private String fromName;

    // Champs SMTP (null si providerType = BREVO)
    private String  smtpHost;
    private Integer smtpPort;
    private String  smtpUsername;
    private boolean smtpSslEnabled;
    private boolean smtpTlsEnabled;

    // Presence des secrets — jamais les valeurs chiffrees
    private boolean hasSmtpPassword;
    private boolean hasBrevoApiKey;
    private boolean hasBrevoWebhookToken;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EmailProviderConfigResponse from(EmailProviderConfig c) {
        return EmailProviderConfigResponse.builder()
                .id(c.getId())
                .providerType(c.getProviderType())
                .active(c.isActive())
                .fromEmail(c.getFromEmail())
                .fromName(c.getFromName())
                .smtpHost(c.getSmtpHost())
                .smtpPort(c.getSmtpPort())
                .smtpUsername(c.getSmtpUsername())
                .smtpSslEnabled(c.isSmtpSslEnabled())
                .smtpTlsEnabled(c.isSmtpTlsEnabled())
                .hasSmtpPassword(c.getSmtpPasswordEncrypted() != null)
                .hasBrevoApiKey(c.getBrevoApiKeyEncrypted() != null)
                .hasBrevoWebhookToken(c.getBrevoWebhookTokenEncrypted() != null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
