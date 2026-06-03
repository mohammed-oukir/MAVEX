package com.medafrica.mavex.dto.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GatewayConfigResponse {

    private Long id;
    private PaymentGatewayType type;
    private String name;

    /** Clés masquées pour l'UI — ex: ****hlvc */
    private String apiKeyMasked;
    private String secretKeyMasked;
    private boolean webhookSecretSet;

    private PaymentGatewayMode mode;
    private boolean active;
    private String supportedCurrencies;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Masque une clé — affiche les 4 derniers chars */
    public static String mask(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.length() <= 4) return "****";
        return "****" + key.substring(key.length() - 4);
    }
}
