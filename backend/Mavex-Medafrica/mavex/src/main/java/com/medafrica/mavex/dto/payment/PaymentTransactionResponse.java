package com.medafrica.mavex.dto.payment;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentTransactionResponse {

    private Long              id;
    private String            gatewayRef;
    private PaymentGatewayType gateway;
    private BigDecimal        amount;
    private String            currency;
    private PaymentStatus     status;
    private String            ipAddress;
    private LocalDateTime     paidAt;
    private LocalDateTime     createdAt;
}
