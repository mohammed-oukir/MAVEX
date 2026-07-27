package com.medafrica.mavex.dto.payment;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ManualPaymentConfigResponse {
    private String ribFileName;
    private LocalDateTime uploadedAt;
}
