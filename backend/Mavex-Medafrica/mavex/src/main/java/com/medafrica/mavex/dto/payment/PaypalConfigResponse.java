package com.medafrica.mavex.dto.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PaypalConfigResponse {

    private String clientId;
    private String webhookId;
    private PaymentGatewayMode mode;
    private LocalDateTime updatedAt;
}
