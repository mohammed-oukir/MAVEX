package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.order.BulkStatusResult;
import com.medafrica.mavex.dto.order.OrderPatchRequest;
import com.medafrica.mavex.dto.order.OrderRequest;
import com.medafrica.mavex.dto.order.OrderResponse;
import com.medafrica.mavex.dto.order.OrderStatusUpdateRequest;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.OrderStatusHistory;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.ClientRepository;
import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.OrderStatusHistoryRepository;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final PaymentTransactionRepository transactionRepository;

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
    public Page<OrderResponse> search(String hawb, String client, String clientEmail,
                                       String shipmentSearch,
                                       Double shipmentWeight, Double customsValue,
                                       Double totalAmount, Double dutyRate,
                                       String customsCurrency, OrderStatus status,
                                       Long shipmentId, LocalDate from, LocalDate to,
                                       Pageable pageable) {
        Specification<Order> spec = OrderSpecification.build(
                hawb, client, clientEmail, shipmentSearch, shipmentWeight, customsValue,
                totalAmount, dutyRate, customsCurrency, status, shipmentId, from, to);
        Page<Order> page = orderRepository.findAll(spec, pageable);

        Map<Long, EmailLog> lastSentByOrderId = lastSentEmailLogsByOrders(page.getContent());

        return page.map(order -> toResponse(order, lastSentByOrderId.get(order.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> searchAll(String hawb, String client, String clientEmail,
                                  String shipmentSearch,
                                  Double shipmentWeight, Double customsValue,
                                  Double totalAmount, Double dutyRate,
                                  String customsCurrency, OrderStatus status,
                                  Long shipmentId, LocalDate from, LocalDate to) {
        Specification<Order> spec = OrderSpecification.build(
                hawb, client, clientEmail, shipmentSearch, shipmentWeight, customsValue,
                totalAmount, dutyRate, customsCurrency, status, shipmentId, from, to);
        return orderRepository.findAll(spec, Pageable.unpaged()).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByIds(List<Long> ids) {
        return orderRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByShipment(Long shipmentId) {
        List<Order> orders = orderRepository.findByShipmentIdWithRelations(shipmentId);
        Map<Long, EmailLog> lastSentByOrderId = lastSentEmailLogsByOrders(orders);
        return orders.stream()
                .map(o -> toResponse(o, lastSentByOrderId.get(o.getId())))
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

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Impossible de modifier cette commande : elle a déjà été payée.");
        }

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

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Impossible de modifier cette commande : elle a déjà été payée.");
        }

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
            Long ancienClientId = order.getClient() != null ? order.getClient().getId() : null;
            boolean clientChanged = !request.getClientId().equals(ancienClientId);

            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client introuvable id=" + request.getClientId()));
            order.setClient(client);

            if (clientChanged && order.getStatus() == OrderStatus.EMAIL_SENT) {
                order.setStatus(OrderStatus.EMAIL_OUTDATED);
                order.setEmailOutdatedReason("Le client de cet order a changé depuis le dernier envoi.");
            }
        }

        return toResponse(orderRepository.save(order));
    }

    // ─────────────────────── CHANGEMENT DE STATUT ─────────────────────

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = findOrThrow(id);
        changeStatus(order, request.getNewStatus(), request.getNote(), getCurrentUser());
        return toResponse(order);
    }

    @Override
    @Transactional
    public BulkStatusResult bulkUpdateStatus(List<Long> ids, OrderStatus newStatus, String note) {
        int succeeded = 0, failed = 0;
        for (Long id : ids) {
            try {
                Order order = findOrThrow(id);
                changeStatus(order, newStatus, note, getCurrentUser());
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.warn("bulkUpdateStatus — erreur order {} : {}", id, e.getMessage());
            }
        }
        return BulkStatusResult.builder()
                .total(ids.size())
                .succeeded(succeeded)
                .failed(failed)
                .build();
    }

    @Override
    @Transactional
    public void updateStatusSystem(Long orderId, OrderStatus newStatus, String note) {
        Order order = findOrThrow(orderId);
        changeStatus(order, newStatus, note, null);
    }

    /**
     * Logique centrale de changement de statut — utilisee par updateStatus(),
     * bulkUpdateStatus() et updateStatusSystem().
     * Cree automatiquement une PaymentTransaction si le statut passe manuellement a PAID.
     */
    private void changeStatus(Order order, OrderStatus newStatus, String note, User actor) {
        if (transactionRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {
            throw new IllegalStateException("Cette commande a déjà été payée. Le statut ne peut plus être modifié.");
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        recordHistory(order, previous, newStatus, note, actor);

        if (newStatus == OrderStatus.PAID && previous != OrderStatus.PAID) {
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .gateway(PaymentGatewayType.MANUEL)
                    .status(PaymentStatus.SUCCESS)
                    .amount(order.getTotalAmount())
                    .currency("USD")
                    .paidAt(LocalDateTime.now())
                    .order(order)
                    .build();
            transactionRepository.save(transaction);
        }
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
        if (transactionRepository.existsByOrder_Id(id)) {
            throw new IllegalStateException("Impossible de supprimer cette commande : une tentative de paiement y est associée.");
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

    private Map<Long, EmailLog> lastSentEmailLogsByOrders(List<Order> orders) {
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return emailLogRepository.findLastSentByOrderIds(orderIds, EmailStatus.SENT).stream()
                .collect(Collectors.toMap(
                        e -> e.getOrder().getId(),
                        e -> e,
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));
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
        EmailLog lastLog = o.getId() != null
                ? emailLogRepository.findTopByOrderIdAndStatusOrderBySentAtDesc(
                        o.getId(), EmailStatus.SENT).orElse(null)
                : null;
        return toResponse(o, lastLog);
    }

    private OrderResponse toResponse(Order o, EmailLog lastLog) {
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
                .emailSentCount(o.getEmailSentCount())
                .emailSentToAddress(o.getEmailSentToAddress())
                .emailOutdatedReason(o.getEmailOutdatedReason())
                .deliveredAt(lastLog != null ? lastLog.getDeliveredAt() : null)
                .openedAt(lastLog != null ? lastLog.getOpenedAt() : null)
                .clickedAt(lastLog != null ? lastLog.getClickedAt() : null)
                .bouncedAt(lastLog != null ? lastLog.getBouncedAt() : null)
                .bounceReason(lastLog != null ? lastLog.getBounceReason() : null)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .lockedExchangeRate(o.getLockedExchangeRate())
                .lockedToCurrency(o.getLockedToCurrency())
                .lockedAmountMAD(
                    o.getTotalAmount() != null && o.getLockedExchangeRate() != null
                        ? o.getTotalAmount().multiply(o.getLockedExchangeRate())
                               .setScale(2, RoundingMode.HALF_UP)
                        : null
                )
                .build();
    }
}
