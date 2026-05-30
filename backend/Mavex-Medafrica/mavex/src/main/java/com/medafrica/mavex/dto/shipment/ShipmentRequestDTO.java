package com.medafrica.mavex.dto.shipment;

import com.medafrica.mavex.model.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ShipmentRequestDTO {

    @NotBlank(message = "Le MAWB est obligatoire")
    private String mawb;

    private LocalDate exportDate;
    private LocalDate importDate;
    private String importingCarrier;
    private String modeOfTransport;
    private String portCode;

    /** ID du shipper lié */
    private Long shipperId;

    /** Statut — optionnel au PATCH */
    private ShipmentStatus status;
}