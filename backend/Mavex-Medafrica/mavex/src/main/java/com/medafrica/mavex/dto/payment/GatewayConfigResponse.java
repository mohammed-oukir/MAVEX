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

    private PaymentGatewayMode mode;
    private boolean active;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
