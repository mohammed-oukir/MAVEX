package com.medafrica.mavex.service;

import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Recalcule le statut d'un Shipment a partir de l'etat de ses Orders.
 * Point d'entree unique pour cette regle metier — appele depuis tous les
 * endroits ou le statut d'un Order change (creation, suppression,
 * changement de statut, envoi email, paiement).
 *
 * Le recalcul est TOTAL a chaque appel (pas d'historique) : un Shipment
 * CLOSED peut redevenir PROCESSING si un nouvel Order lui est ajoute.
 * DRAFT et IMPORTED ne sont jamais positionnes par cette methode.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentStatusService {

    private static final Set<OrderStatus> CLOSING_STATUSES = Set.of(OrderStatus.PAID, OrderStatus.CANCELLED);

    private final OrderRepository    orderRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public void recalculate(Long shipmentId) {
        List<Order> orders = orderRepository.findByShipmentId(shipmentId);

        // Regle 2 : liste vide -> on ne touche pas au statut.
        if (orders.isEmpty()) {
            return;
        }

        ShipmentStatus computed;
        if (orders.stream().allMatch(o -> CLOSING_STATUSES.contains(o.getStatus()))) {
            // Regle 3 : tous PAID/CANCELLED -> CLOSED.
            computed = ShipmentStatus.CLOSED;
        } else if (orders.stream().anyMatch(o -> o.getStatus() != OrderStatus.CREATED)) {
            // Regle 4 : au moins un Order a quitte CREATED -> PROCESSING.
            computed = ShipmentStatus.PROCESSING;
        } else {
            // Regle 5 : tous encore CREATED -> on ne touche pas au statut (DRAFT/IMPORTED).
            return;
        }

        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null) {
            log.warn("recalculate() — shipment introuvable id={}, statut non recalcule", shipmentId);
            return;
        }

        // Regle 7 : ne sauvegarder que si le statut change reellement.
        if (shipment.getStatus() != computed) {
            ShipmentStatus previous = shipment.getStatus();
            shipment.setStatus(computed);
            shipmentRepository.save(shipment);
            log.info("Shipment id={} statut recalcule {} -> {}", shipmentId, previous, computed);
        }
    }
}
