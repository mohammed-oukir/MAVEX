package com.medafrica.mavex.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCaptureResponse {
    private boolean success;
    private String orderStatus;
}
