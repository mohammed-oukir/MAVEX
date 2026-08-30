package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.order.BulkStatusResult;
import com.medafrica.mavex.dto.order.OrderPatchRequest;
import com.medafrica.mavex.dto.order.OrderRequest;
import com.medafrica.mavex.dto.order.OrderResponse;
import com.medafrica.mavex.dto.order.OrderStatusUpdateRequest;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
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

    /** Recherche avec filtres par colonne, sans pagination — renvoie toutes les lignes filtrées (pour export). */
    List<Order> searchAll(
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
            LocalDate to
    );

    /** Récupère plusieurs orders par leurs ids (pour export d'une sélection). */
    List<Order> findAllByIds(List<Long> ids);

    OrderResponse update(Long id, OrderRequest request);

    OrderResponse patch(Long id, OrderPatchRequest request);

    OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request);

    /** Met à jour le statut de plusieurs orders en une transaction */
    BulkStatusResult bulkUpdateStatus(List<Long> ids, OrderStatus newStatus, String note);

    OrderResponse generatePaymentToken(Long id);

    OrderResponse getByPaymentToken(String token);

    void delete(Long id);
}
