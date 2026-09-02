package com.medafrica.mavex.dto.shipment;

import com.medafrica.mavex.model.enums.DutyChangeEntityType;
import com.medafrica.mavex.model.logistics.DutyChangeHistory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class DutyChangeHistoryResponse {

    private Long                 id;
    private DutyChangeEntityType entityType;
    private Long                 shipmentId;
    private Long                 orderId;
    private String               orderHawb;
    private BigDecimal           oldDutyRate;
    private BigDecimal           newDutyRate;
    private String               changedByName;
    private LocalDateTime        changedAt;

    public static DutyChangeHistoryResponse from(DutyChangeHistory h) {
        return DutyChangeHistoryResponse.builder()
                .id(h.getId())
                .entityType(h.getEntityType())
                .shipmentId(h.getShipment() != null ? h.getShipment().getId() : null)
                .orderId(h.getOrder() != null ? h.getOrder().getId() : null)
                .orderHawb(h.getOrder() != null ? h.getOrder().getHawb() : null)
                .oldDutyRate(h.getOldDutyRate())
                .newDutyRate(h.getNewDutyRate())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                .changedAt(h.getChangedAt())
                .build();
    }
}
