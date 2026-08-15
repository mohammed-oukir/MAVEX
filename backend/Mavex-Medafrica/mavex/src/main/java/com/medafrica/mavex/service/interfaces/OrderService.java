package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.order.BulkStatusResult;
import com.medafrica.mavex.dto.order.OrderPatchRequest;
import com.medafrica.mavex.dto.order.OrderRequest;
import com.medafrica.mavex.dto.order.OrderResponse;
import com.medafrica.mavex.dto.order.OrderStatusUpdateRequest;
import com.medafrica.mavex.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    OrderResponse create(OrderRequest request);

    OrderResponse getById(Long id);

    OrderResponse getByHawb(String hawb);

    /** Recherche paginée avec filtres par colonne */
    Page<OrderResponse> search(
            String hawb,
            String client,
            String clientEmail,
            String shipmentSearch,
            Double shipmentWeight,
            Double customsValue,
            Double totalAmount,
            Double dutyRate,
            String customsCurrency,
            OrderStatus status,
            Long shipmentId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    List<OrderResponse> getByShipment(Long shipmentId);

    List<OrderResponse> getByClient(Long clientId);

    OrderResponse update(Long id, OrderRequest request);

    OrderResponse patch(Long id, OrderPatchRequest request);

    OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request);

    void updateStatusSystem(Long orderId, OrderStatus newStatus, String note);

    /** Met à jour le statut de plusieurs orders en une transaction */
    BulkStatusResult bulkUpdateStatus(List<Long> ids, OrderStatus newStatus, String note);

    OrderResponse generatePaymentToken(Long id);

    OrderResponse getByPaymentToken(String token);

    void delete(Long id);
}
