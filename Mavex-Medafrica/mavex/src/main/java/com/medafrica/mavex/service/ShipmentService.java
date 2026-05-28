package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {
    

    private final ShipmentRepository           shipmentRepository;
    private final ShipperRepository            shipperRepository;
    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final EmailLogRepository           emailLogRepository;

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------
    @Transactional
    public ShipmentResponseDTO create(ShipmentRequestDTO req) {
        if (shipmentRepository.existsByMawb(req.getMawb())) {
            throw new IllegalArgumentException("MAWB déjà existant : " + req.getMawb());
        }
        Shipper shipper = null;
        if (req.getShipperId() != null) {
            shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
        }
        Shipment shipment = Shipment.builder()
                .mawb(req.getMawb())
                .importingCarrier(req.getImportingCarrier())
                .modeOfTransport(req.getModeOfTransport())
                .portCode(req.getPortCode())
                .shipper(shipper)
                .createdBy(currentUser())
                .build();
        return toResponse(shipmentRepository.save(shipment));
    }

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public ShipmentResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> list(Pageable pageable) {
        return shipmentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> listByStatus(ShipmentStatus status, Pageable pageable) {
        return shipmentRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // UPDATE - PATCH
    // ---------------------------------------------------------------
    @Transactional
    public ShipmentResponseDTO update(Long id, ShipmentRequestDTO req) {
        Shipment shipment = findOrThrow(id);
        if (req.getMawb() != null && !req.getMawb().equalsIgnoreCase(shipment.getMawb())) {
            if (shipmentRepository.existsByMawb(req.getMawb())) {
                throw new IllegalArgumentException("MAWB déjà existant : " + req.getMawb());
            }
            shipment.setMawb(req.getMawb());
        }
        if (req.getImportingCarrier() != null) shipment.setImportingCarrier(req.getImportingCarrier());
        if (req.getModeOfTransport()  != null) shipment.setModeOfTransport(req.getModeOfTransport());
        if (req.getPortCode()         != null) shipment.setPortCode(req.getPortCode());
        if (req.getShipperId() != null) {
            Shipper shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
            shipment.setShipper(shipper);
        }
        return toResponse(shipmentRepository.save(shipment));
    }

    // ---------------------------------------------------------------
    // UPDATE - statut
    // ---------------------------------------------------------------
    @Transactional
    public ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO req) {
        Shipment shipment = findOrThrow(id);
        shipment.setStatus(req.getNewStatus());
        return toResponse(shipmentRepository.save(shipment));
    }

    // ---------------------------------------------------------------
    // UPDATE - PUT
    // ---------------------------------------------------------------
    @Transactional
    public ShipmentResponseDTO replace(Long id, ShipmentRequestDTO req) {
        Shipment shipment = findOrThrow(id);
        if (!req.getMawb().equalsIgnoreCase(shipment.getMawb()) &&
             shipmentRepository.existsByMawb(req.getMawb())) {
            throw new IllegalArgumentException("MAWB déjà existant : " + req.getMawb());
        }
        Shipper shipper = null;
        if (req.getShipperId() != null) {
            shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
        }
        shipment.setMawb(req.getMawb());
        shipment.setImportingCarrier(req.getImportingCarrier());
        shipment.setModeOfTransport(req.getModeOfTransport());
        shipment.setPortCode(req.getPortCode());
        shipment.setShipper(shipper);
        return toResponse(shipmentRepository.save(shipment));
    }

    // ---------------------------------------------------------------
    // DELETE — supprime le shipment ET tous ses orders liés
    // ---------------------------------------------------------------
    @Transactional
    public void delete(Long id) {
        Shipment shipment = findOrThrow(id);

        // 1. Récupérer tous les orders de ce shipment
        List<Order> orders = orderRepository.findByShipmentId(id);
        log.info("Suppression shipment {} ({}) — {} orders liés", id, shipment.getMawb(), orders.size());

        for (Order order : orders) {
            // 2a. Supprimer les email logs liés à cet order
              emailLogRepository.deleteByOrderId(order.getId());

              //paymentTransactionRepository.deleteByOrderId(order.getId());

            // 2b. Supprimer l'historique des statuts
          orderStatusHistoryRepository.deleteByOrderId(order.getId()); // ← corrigé
        }

        // 3. Supprimer tous les orders
        orderRepository.deleteAll(orders);

        // 4. Supprimer le shipment
        shipmentRepository.delete(shipment);

        log.info("Shipment {} supprimé avec {} orders", shipment.getMawb(), orders.size());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private Shipment findOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment introuvable : " + id));
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) return u;
        return null;
    }

    private ShipmentResponseDTO toResponse(Shipment s) {
        ShipmentResponseDTO.ShipperSummary shipperSummary = null;
        if (s.getShipper() != null) {
            shipperSummary = ShipmentResponseDTO.ShipperSummary.builder()
                    .id(s.getShipper().getId())
                    .companyName(s.getShipper().getCompanyName())
                    .email(s.getShipper().getEmail())
                    .build();
        }
        return ShipmentResponseDTO.builder()
                .id(s.getId())
                .mawb(s.getMawb())
                .importingCarrier(s.getImportingCarrier())
                .modeOfTransport(s.getModeOfTransport())
                .portCode(s.getPortCode())
                .status(s.getStatus())
                .totalOrders(s.getOrders().size())
                .shipper(shipperSummary)
                .createdBy(s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : "system")
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}