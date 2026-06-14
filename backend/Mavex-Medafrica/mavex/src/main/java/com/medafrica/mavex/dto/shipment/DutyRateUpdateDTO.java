package com.medafrica.mavex.dto.shipment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DutyRateUpdateDTO {

    @NotNull(message = "Le taux douanier est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le taux ne peut pas être négatif")
    @DecimalMax(value = "1.0",   message = "Le taux ne peut pas dépasser 100% (1.0)")
    private BigDecimal dutyRate;
}
