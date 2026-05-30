package com.medafrica.mavex.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AlertsDTO {
    private long ordersNeverEmailed;
    private long stuckShipments;
    private long overduePayments;

    public boolean hasAlerts() {
        return ordersNeverEmailed > 0 || stuckShipments > 0 || overduePayments > 0;
    }
}
