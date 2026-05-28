package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.order.OrderStatusHistoryResponse;

import java.util.List;

public interface OrderStatusHistoryService {

    List<OrderStatusHistoryResponse> getHistoryByOrder(Long orderId);

    List<OrderStatusHistoryResponse> getHistoryByUser(Long userId);
}
