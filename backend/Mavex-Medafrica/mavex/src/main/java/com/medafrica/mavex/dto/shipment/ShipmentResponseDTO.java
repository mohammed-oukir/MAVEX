package com.medafrica.mavex.dto.shipment;

import com.medafrica.mavex.model.enums.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipmentResponseDTO {

    private Long id;
    private String mawb;
    private LocalDate exportDate;
    private LocalDate importDate;
    private String importingCarrier;
    private String modeOfTransport;
    private String portCode;
    private ShipmentStatus status;
    private BigDecimal dutyRate;

    /** Nombre d'orders dans ce shipment */
    private int totalOrders;

    /** Résumé du shipper */
    private ShipperSummary shipper;

    /** Agent qui a importé */
    private String createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ShipperSummary {
        private Long id;
        private String companyName;
        private String email;
    }
}