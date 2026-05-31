package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.order.OrderPatchRequest;
import com.medafrica.mavex.dto.order.OrderRequest;
import com.medafrica.mavex.dto.order.OrderResponse;
import com.medafrica.mavex.dto.order.OrderStatusUpdateRequest;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.OrderStatusHistory;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.ClientRepository;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.OrderStatusHistoryRepository;
import com.medafrica.mavex.repository.ShipmentRepository;
import com.medafrica.mavex.repository.specification.OrderSpecification;
import com.medafrica.mavex.service.interfaces.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ShipmentRepository           shipmentRepository;
    private final ClientRepository             clientRepository;
    private final EmailLogRepository           emailLogRepository;

    // ───────────────────────────── CREATE ─────────────────────────────

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request) {

        if (orderRepository.existsByHawb(request.getHawb())) {
            throw new IllegalArgumentException("Un order avec le HAWB '" + request.getHawb() + "' existe déjà.");
        }

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new EntityNotFoundException("Shipment introuvable id=" + request.getShipmentId()));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable id=" + request.getClientId()));

        Order order = Order.builder()
                .hawb(request.getHawb())
                .goodsDescription(request.getGoodsDescription())
                .htsusCode(request.getHtsusCode())
                .numberOfItems(request.getNumberOfItems())
                .shipmentWeight(request.getShipmentWeight())
                .grossWeight(request.getGrossWeight())
                .netQuantity(request.getNetQuantity())
                .manifestQty(request.getManifestQty())
                .customsValue(request.getCustomsValue())
                .customsCurrency(request.getCustomsCurrency() != null ? request.getCustomsCurrency() : "USD")
                .dutyRate(request.getDutyRate() != null ? request.getDutyRate()
                        : (shipment.getDutyRate() != null ? shipment.getDutyRate() : new BigDecimal("0.10")))
                .bankCharges(request.getBankCharges())
                .enteredValue(request.getEnteredValue())
                .shipment(shipment)
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        Order saved = orderRepository.save(order);
        recordHistory(saved, null, OrderStatus.CREATED, "Création de l'order", getCurrentUser());

        return toResponse(saved);
    }

    // ───────────────────────────── READ ───────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByHawb(String hawb) {
        Order order = orderRepository.findByHawb(hawb)
                .orElseThrow(() -> new EntityNotFoundException("Order introuvable HAWB=" + hawb));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> search(String q, OrderStatus status, Long shipmentId,
                                       LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<Order> spec = OrderSpecification.build(q, status, shipmentId, from, to);
        return orderRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByShipment(Long shipmentId) {
        return orderRepository.findByShipmentId(shipmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByClient(Long clientId) {
        return orderRepository.findByClientId(clientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ───────────────────────────── UPDATE ─────────────────────────────

    @Override
    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = findOrThrow(id);

        if (!order.getHawb().equals(request.getHawb()) && orderRepository.existsByHawb(request.getHawb())) {
            throw new IllegalArgumentException("Un order avec le HAWB '" + request.getHawb() + "' existe déjà.");
        }

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new EntityNotFoundException("Shipment introuvable id=" + request.getShipmentId()));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable id=" + request.getClientId()));

        order.setHawb(request.getHawb());
        order.setGoodsDescription(request.getGoodsDescription());
        order.setHtsusCode(request.getHtsusCode());
        order.setNumberOfItems(request.getNumberOfItems());
        order.setShipmentWeight(request.getShipmentWeight());
        order.setGrossWeight(request.getGrossWeight());
        order.setNetQuantity(request.getNetQuantity());
        order.setManifestQty(request.getManifestQty());
        order.setCustomsValue(request.getCustomsValue());
        if (request.getCustomsCurrency() != null) order.setCustomsCurrency(request.getCustomsCurrency());
        if (request.getDutyRate() != null)        order.setDutyRate(request.getDutyRate());
        order.setBankCharges(request.getBankCharges());
        order.setEnteredValue(request.getEnteredValue());
        order.setShipment(shipment);
        order.setClient(client);

        return toResponse(orderRepository.save(order));
    }

    // ─────────────────────── PATCH ────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse patch(Long id, OrderPatchRequest request) {
        Order order = findOrThrow(id);

        if (request.getHawb() != null
                && !request.getHawb().equals(order.getHawb())
                && orderRepository.existsByHawb(request.getHawb())) {
            throw new IllegalArgumentException("Un order avec le HAWB '" + request.getHawb() + "' existe déjà.");
        }

        if (request.getHawb()             != null) order.setHawb(request.getHawb());
        if (request.getGoodsDescription() != null) order.setGoodsDescription(request.getGoodsDescription());
        if (request.getHtsusCode()        != null) order.setHtsusCode(request.getHtsusCode());
        if (request.getNumberOfItems()    != null) order.setNumberOfItems(request.getNumberOfItems());
        if (request.getShipmentWeight()   != null) order.setShipmentWeight(request.getShipmentWeight());
        if (request.getGrossWeight()      != null) order.setGrossWeight(request.getGrossWeight());
        if (request.getNetQuantity()      != null) order.setNetQuantity(request.getNetQuantity());
        if (request.getManifestQty()      != null) order.setManifestQty(request.getManifestQty());
        if (request.getCustomsValue()     != null) order.setCustomsValue(request.getCustomsValue());
        if (request.getCustomsCurrency()  != null) order.setCustomsCurrency(request.getCustomsCurrency());
        if (request.getDutyRate()         != null) order.setDutyRate(request.getDutyRate());
        if (request.getBankCharges()      != null) order.setBankCharges(request.getBankCharges());
        if (request.getEnteredValue()     != null) order.setEnteredValue(request.getEnteredValue());

        if (request.getShipmentId() != null) {
            Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipment introuvable id=" + request.getShipmentId()));
            order.setShipment(shipment);
        }

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client introuvable id=" + request.getClientId()));
            order.setClient(client);
        }

        return toResponse(orderRepository.save(order));
    }

    // ─────────────────────── CHANGEMENT DE STATUT ─────────────────────

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = findOrThrow(id);
        OrderStatus previous = order.getStatus();
        order.setStatus(request.getNewStatus());
        orderRepository.save(order);
        recordHistory(order, previous, request.getNewStatus(), request.getNote(), getCurrentUser());
        return toResponse(order);
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<Long> ids, OrderStatus newStatus, String note) {
        for (Long id : ids) {
            try {
                Order order = findOrThrow(id);
                OrderStatus previous = order.getStatus();
                order.setStatus(newStatus);
                orderRepository.save(order);
                recordHistory(order, previous, newStatus, note, getCurrentUser());
            } catch (Exception e) {
                log.warn("bulkUpdateStatus — erreur order {} : {}", id, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void updateStatusSystem(Long orderId, OrderStatus newStatus, String note) {
        Order order = findOrThrow(orderId);
        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        recordHistory(order, previous, newStatus, note, null);
    }

    // ─────────────────────── TOKEN DE PAIEMENT ─────────────────────────

    @Override
    @Transactional
    public OrderResponse generatePaymentToken(Long id) {
        Order order = findOrThrow(id);
        order.generatePaymentToken();
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByPaymentToken(String token) {
        Order order = orderRepository.findByPaymentToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Lien de paiement invalide ou expiré."));
        if (!order.isTokenValid()) {
            throw new IllegalStateException("Le lien de paiement a expiré.");
        }
        return toResponse(order);
    }

    // ───────────────────────────── DELETE ─────────────────────────────

    @Override
    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new EntityNotFoundException("Order introuvable id=" + id);
        }
        emailLogRepository.deleteByOrderId(id);
        historyRepository.deleteByOrderId(id);
        orderRepository.deleteById(id);
    }

    // ───────────────────────────── HELPERS ────────────────────────────

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order introuvable id=" + id));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u : null;
    }

    private void recordHistory(Order order, OrderStatus from, OrderStatus to, String note, User changedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(to)
                .note(note)
                .changedBy(changedBy)
                .build();
        historyRepository.save(history);
    }

    // ─────────────────────────── MAPPING ──────────────────────────────

    private OrderResponse toResponse(Order o) {

        String clientFullName = null;
        String clientEmail    = null;
        String clientPhone    = null;
        String clientAddress  = null;
        String clientCity     = null;
        String clientState    = null;
        String clientZipCode  = null;
        String clientCountry  = null;

        if (o.getClient() != null) {
            Client c    = o.getClient();
            clientFullName = c.getFullName();
            clientEmail    = c.getEmail();
            clientPhone    = c.getPhone();
            clientAddress  = c.getAddress();
            clientCity     = c.getCity();
            clientState    = c.getState();
            clientZipCode  = c.getZipCode();
            clientCountry  = c.getCountry() != null ? c.getCountry().getCode() : null;
        }

        String mawb        = null;
        String companyName = null;

        if (o.getShipment() != null) {
            mawb = o.getShipment().getMawb();
            if (o.getShipment().getShipper() != null) {
                companyName = o.getShipment().getShipper().getCompanyName();
            }
        }

        return OrderResponse.builder()
                .id(o.getId())
                .hawb(o.getHawb())
                .goodsDescription(o.getGoodsDescription())
                .htsusCode(o.getHtsusCode())
                .numberOfItems(o.getNumberOfItems())
                .shipmentWeight(o.getShipmentWeight())
                .grossWeight(o.getGrossWeight())
                .netQuantity(o.getNetQuantity())
                .manifestQty(o.getManifestQty())
                .customsValue(o.getCustomsValue())
                .customsCurrency(o.getCustomsCurrency())
                .dutyRate(o.getDutyRate())
                .dutyAmount(o.getDutyAmount())
                .bankCharges(o.getBankCharges())
                .totalAmount(o.getTotalAmount())
                .enteredValue(o.getEnteredValue())
                .status(o.getStatus())
                .paymentToken(o.getPaymentToken())
                .tokenExpiresAt(o.getTokenExpiresAt())
                .tokenValid(o.isTokenValid())
                .shipmentId(o.getShipment() != null ? o.getShipment().getId() : null)
                .mawb(mawb)
                .clientId(o.getClient() != null ? o.getClient().getId() : null)
                .clientFullName(clientFullName)
                .clientEmail(clientEmail)
                .clientPhone(clientPhone)
                .clientAddress(clientAddress)
                .clientCity(clientCity)
                .clientState(clientState)
                .clientZipCode(clientZipCode)
                .clientCountry(clientCountry)
                .companyName(companyName)
                .emailSentAt(o.getEmailSentAt())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
