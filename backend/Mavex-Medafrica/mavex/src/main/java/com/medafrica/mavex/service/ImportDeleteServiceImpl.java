package com.medafrica.mavex.service;

import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.model.imports.ImportRowLog;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.repository.*;
import com.medafrica.mavex.service.interfaces.ImportDeleteService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportDeleteServiceImpl implements ImportDeleteService {

    private final ImportLogRepository          importLogRepository;
    private final ImportRowLogRepository       importRowLogRepository;
    private final OrderRepository              orderRepository;
    private final ShipmentRepository           shipmentRepository;
    private final ClientRepository             clientRepository;
    private final EmailLogRepository           emailLogRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Override
    @Transactional
    public void deleteImport(Long importLogId) {

        ImportLog importLog = importLogRepository.findById(importLogId)
            .orElseThrow(() -> new EntityNotFoundException("Import introuvable id=" + importLogId));

        log.info("Suppression de l'import {} ({})", importLogId, importLog.getFileName());

        List<String> importedHawbs = importLog.getRowLogs().stream()
            .filter(r -> r.getStatus() != null &&
                         r.getStatus().name().equals("IMPORTED"))
            .map(ImportRowLog::getHawb)
            .filter(h -> h != null && !h.isBlank())
            .collect(Collectors.toList());

        log.info("HAWBs à supprimer : {}", importedHawbs);

        List<Order> ordersToDelete = orderRepository.findAllByHawbIn(importedHawbs);

        Set<Long> shipmentIds = ordersToDelete.stream()
            .filter(o -> o.getShipment() != null)
            .map(o -> o.getShipment().getId())
            .collect(Collectors.toSet());

        Set<Long> clientIds = ordersToDelete.stream()
            .filter(o -> o.getClient() != null)
            .map(o -> o.getClient().getId())
            .collect(Collectors.toSet());

        if (!ordersToDelete.isEmpty()) {
            List<Long> orderIds = ordersToDelete.stream().map(Order::getId).collect(Collectors.toList());
            if (orderRepository.existsByIdInAndStatusIn(orderIds,
                    List.of(OrderStatus.EMAIL_SENT, OrderStatus.EMAIL_OUTDATED, OrderStatus.PAID))) {
                throw new IllegalStateException("Impossible de supprimer cet import : au moins une commande a déjà reçu un email ou a été payée.");
            }
        }

        if (!ordersToDelete.isEmpty()) {
            for (Order order : ordersToDelete) {
                emailLogRepository.deleteByOrderId(order.getId());
                orderStatusHistoryRepository.deleteByOrderId(order.getId());
            }
            orderRepository.deleteAll(ordersToDelete);
            log.info("{} orders supprimés", ordersToDelete.size());
        }

        for (Long shipmentId : shipmentIds) {
            long remaining = orderRepository.countByShipmentId(shipmentId);
            if (remaining == 0) {
                shipmentRepository.deleteById(shipmentId);
                log.info("Shipment {} supprimé (plus d'orders)", shipmentId);
            } else {
                log.info("Shipment {} conservé ({} orders restants)", shipmentId, remaining);
            }
        }

        for (Long clientId : clientIds) {
            long remaining = orderRepository.countByClientId(clientId);
            if (remaining == 0) {
                clientRepository.deleteById(clientId);
                log.info("Client {} supprimé (plus d'orders)", clientId);
            } else {
                log.info("Client {} conservé ({} orders restants)", clientId, remaining);
            }
        }

        importRowLogRepository.deleteAll(importLog.getRowLogs());
        importLogRepository.delete(importLog);

        log.info("Import {} supprimé complètement ✓", importLogId);
    }
}
