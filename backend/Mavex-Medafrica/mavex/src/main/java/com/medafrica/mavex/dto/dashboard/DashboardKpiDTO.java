package com.medafrica.mavex.dto.dashboard;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class DashboardKpiDTO {
    private long totalShipments;
    private long activeShipments;
    private long totalOrders;
    private long paidOrders;
    private long pendingOrders;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalPendingAmount;
}
