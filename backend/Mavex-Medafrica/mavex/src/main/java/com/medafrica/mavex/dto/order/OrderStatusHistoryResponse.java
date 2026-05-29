package com.medafrica.mavex.dto.order;

import com.medafrica.mavex.model.enums.OrderStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusHistoryResponse {

    private Long id;
    private Long orderId;
    private String hawb;           // pour affichage rapide
    private OrderStatus fromStatus;
    private OrderStatus toStatus;

    /** null si changement automatique (webhook / scheduler) */
    private Long changedById;
    private String changedByEmail;

    private String note;
    private LocalDateTime changedAt;
}