package com.medafrica.mavex.dto.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaypalConfigRequest {

    @NotBlank(message = "Le client ID est obligatoire")
    private String clientId;

    /** Optionnel — si vide, le secret existant n'est pas modifie */
    private String clientSecret;

    private String webhookId;

    @NotNull(message = "Le mode est obligatoire")
    private PaymentGatewayMode mode;
}
