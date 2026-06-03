package com.medafrica.mavex.dto.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GatewayConfigRequest {

    @NotNull(message = "Le type de gateway est obligatoire")
    private PaymentGatewayType type;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    private String apiKey;
    private String secretKey;
    private String webhookSecret;

    @NotNull(message = "Le mode est obligatoire")
    private PaymentGatewayMode mode;

    private String supportedCurrencies;
    private String description;
}
