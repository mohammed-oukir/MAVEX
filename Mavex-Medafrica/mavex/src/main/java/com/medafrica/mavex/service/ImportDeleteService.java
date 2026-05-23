package com.medafrica.mavex.service;

import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.model.imports.ImportRowLog;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.repository.*;
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
public class ImportDeleteService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;
    private final OrderRepository        orderRepository;
    private final ShipmentRepository     shipmentRepository;
    private final ClientRepository       clientRepository;

    /**
     * Supprime un import COMPLÈTEMENT :
     * - Les ImportRowLogs
     * - Les Orders créés par cet import (via HAWB)
     * - Les Shipments qui n'ont plus d'orders
     * - Les Clients qui n'ont plus d'orders
     * - L'ImportLog lui-même
     *
     * Résultat : comme si l'import n'avait jamais eu lieu.
     */
    @Transactional
    public void deleteImport(Long importLogId) {

        // 1. Récupérer le log
        ImportLog importLog = importLogRepository.findById(importLogId)
            .orElseThrow(() -> new EntityNotFoundException("Import introuvable id=" + importLogId));

        log.info("Suppression de l'import {} ({})", importLogId, importLog.getFileName());

        // 2. Récupérer les HAWBs importés depuis les row logs
        List<String> importedHawbs = importLog.getRowLogs().stream()
            .filter(r -> r.getStatus() != null &&
                         r.getStatus().name().equals("IMPORTED"))
            .map(ImportRowLog::getHawb)
            .filter(h -> h != null && !h.isBlank())
            .collect(Collectors.toList());

        log.info("HAWBs à supprimer : {}", importedHawbs);

        // 3. Récupérer les orders concernés
        List<Order> ordersToDelete = orderRepository.findAllByHawbIn(importedHawbs);

        // 4. Collecter les shipment IDs et client IDs AVANT suppression
        Set<Long> shipmentIds = ordersToDelete.stream()
            .filter(o -> o.getShipment() != null)
            .map(o -> o.getShipment().getId())
            .collect(Collectors.toSet());

        Set<Long> clientIds = ordersToDelete.stream()
            .filter(o -> o.getClient() != null)
            .map(o -> o.getClient().getId())
            .collect(Collectors.toSet());

        // 5. Supprimer les orders
        if (!ordersToDelete.isEmpty()) {
            orderRepository.deleteAll(ordersToDelete);
            log.info("{} orders supprimés", ordersToDelete.size());
        }

        // 6. Supprimer les shipments qui n'ont plus d'orders
        for (Long shipmentId : shipmentIds) {
            long remaining = orderRepository.countByShipmentId(shipmentId);
            if (remaining == 0) {
                shipmentRepository.deleteById(shipmentId);
                log.info("Shipment {} supprimé (plus d'orders)", shipmentId);
            } else {
                log.info("Shipment {} conservé ({} orders restants)", shipmentId, remaining);
            }
        }

        // 7. Supprimer les clients qui n'ont plus d'orders
        for (Long clientId : clientIds) {
            long remaining = orderRepository.countByClientId(clientId);
            if (remaining == 0) {
                clientRepository.deleteById(clientId);
                log.info("Client {} supprimé (plus d'orders)", clientId);
            } else {
                log.info("Client {} conservé ({} orders restants)", clientId, remaining);
            }
        }

        // 8. Supprimer les row logs puis le log principal
        importRowLogRepository.deleteAll(importLog.getRowLogs());
        importLogRepository.delete(importLog);

        log.info("Import {} supprimé complètement ✓", importLogId);
    }
}