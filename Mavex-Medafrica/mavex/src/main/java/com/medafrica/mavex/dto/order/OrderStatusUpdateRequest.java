package com.medafrica.mavex.dto.order;

import com.medafrica.mavex.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusUpdateRequest {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private OrderStatus newStatus;

    /** Optionnel : commentaire sur le changement */
    private String note;
}