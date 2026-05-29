package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.order.OrderPatchRequest;
import com.medafrica.mavex.dto.order.OrderRequest;
import com.medafrica.mavex.dto.order.OrderResponse;
import com.medafrica.mavex.dto.order.OrderStatusUpdateRequest;
import com.medafrica.mavex.model.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse create(OrderRequest request);

    OrderResponse getById(Long id);

    OrderResponse getByHawb(String hawb);

    List<OrderResponse> getAll();

    List<OrderResponse> getByShipment(Long shipmentId);

    List<OrderResponse> getByClient(Long clientId);

    List<OrderResponse> getByStatus(OrderStatus status);

    OrderResponse update(Long id, OrderRequest request);

    OrderResponse patch(Long id, OrderPatchRequest request);

    OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request);

    void updateStatusSystem(Long orderId, OrderStatus newStatus, String note);

    OrderResponse generatePaymentToken(Long id);

    OrderResponse getByPaymentToken(String token);

    void delete(Long id);
}
