package com.medafrica.mavex.dto.shipment;

import com.medafrica.mavex.model.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipmentStatusUpdateDTO {

    @NotNull(message = "Le statut est obligatoire")
    private ShipmentStatus newStatus;
}