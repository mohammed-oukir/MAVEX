package com.medafrica.mavex.dto.order;

import com.medafrica.mavex.model.enums.OrderStatus;
import lombok.Data;
import java.util.List;

@Data
public class BulkStatusRequest {
    private List<Long> ids;
    private OrderStatus newStatus;
    private String note;
}
