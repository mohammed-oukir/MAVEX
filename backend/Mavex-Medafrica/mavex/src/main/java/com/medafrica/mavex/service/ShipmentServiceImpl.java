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
import com.medafrica.mavex.repository.specification.ShipmentSpecification;
import com.medafrica.mavex.service.interfaces.ShipmentService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository           shipmentRepository;
    private final ShipperRepository            shipperRepository;
    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final EmailLogRepository           emailLogRepository;

    @Override
    @Transactional
    public ShipmentResponseDTO create(ShipmentRequestDTO req) {
        if (req.getMawb() != null && shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(req.getMawb()).isPresent()) {
            throw new IllegalArgumentException("Un shipment avec le MAWB '" + req.getMawb() + "' existe déjà.");
        }
        Shipper shipper = null;
        if (req.getShipperId() != null) {
            shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
            if (!shipper.isActive()) {
                throw new IllegalArgumentException("Le shipper '" + shipper.getCompanyName() + "' est désactivé. Veuillez le réactiver avant de créer un shipment.");
            }
        }
        Shipment shipment = Shipment.builder()
                .mawb(req.getMawb())
                .exportDate(req.getExportDate())
                .importDate(req.getImportDate())
                .importingCarrier(req.getImportingCarrier())
                .modeOfTransport(req.getModeOfTransport())
                .portCode(req.getPortCode())
                .shipper(shipper)
                .createdBy(currentUser())
                .build();
        return toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> list(Pageable pageable) {
        Page<Shipment> page = shipmentRepository.findAll(pageable);
        Map<Long, Long> orderCounts = countOrdersByShipmentIds(page);
        return page.map(s -> toResponse(s, orderCounts.getOrDefault(s.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> listByStatus(ShipmentStatus status, Pageable pageable) {
        Page<Shipment> page = shipmentRepository.findByStatus(status, pageable);
        Map<Long, Long> orderCounts = countOrdersByShipmentIds(page);
        return page.map(s -> toResponse(s, orderCounts.getOrDefault(s.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> search(String mawb, String shipperCompanyName,
                                            LocalDate importFrom, LocalDate importTo,
                                            String carrier, String mode, Integer totalOrders,
                                            Double dutyRateMin, Double dutyRateMax,
                                            ShipmentStatus status, Pageable pageable) {
        Specification<Shipment> spec = ShipmentSpecification.build(
                mawb, shipperCompanyName, importFrom, importTo, carrier, mode,
                totalOrders, dutyRateMin, dutyRateMax, status);
        Page<Shipment> page = shipmentRepository.findAll(spec, pageable);
        Map<Long, Long> orderCounts = countOrdersByShipmentIds(page);
        return page.map(s -> toResponse(s, orderCounts.getOrDefault(s.getId(), 0L)));
    }

    @Override
    @Transactional
    public ShipmentResponseDTO update(Long id, ShipmentRequestDTO req) {
        Shipment shipment = findOrThrow(id);
        if (req.getMawb() != null) {
            shipment.setMawb(req.getMawb());
        }
        if (req.getExportDate()       != null) shipment.setExportDate(req.getExportDate());
        if (req.getImportDate()       != null) shipment.setImportDate(req.getImportDate());
        if (req.getImportingCarrier() != null) shipment.setImportingCarrier(req.getImportingCarrier());
        if (req.getModeOfTransport()  != null) shipment.setModeOfTransport(req.getModeOfTransport());
        if (req.getPortCode()         != null) shipment.setPortCode(req.getPortCode());
        // shipperId null = retirer le shipper ; présent = l'affecter
        if (req.getShipperId() != null) {
            Shipper shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
            shipment.setShipper(shipper);
        } else {
            shipment.setShipper(null);
        }
        if (req.getStatus() != null) shipment.setStatus(req.getStatus());
        return toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional
    public ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO req) {
        Shipment shipment = findOrThrow(id);
        shipment.setStatus(req.getNewStatus());
        return toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional
    public ShipmentResponseDTO replace(Long id, ShipmentRequestDTO req) {
        Shipment shipment = findOrThrow(id);
        Shipper shipper = null;
        if (req.getShipperId() != null) {
            shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
        }
        shipment.setMawb(req.getMawb());
        shipment.setExportDate(req.getExportDate());
        shipment.setImportDate(req.getImportDate());
        shipment.setImportingCarrier(req.getImportingCarrier());
        shipment.setModeOfTransport(req.getModeOfTransport());
        shipment.setPortCode(req.getPortCode());
        shipment.setShipper(shipper);
        return toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional
    public ShipmentResponseDTO updateDutyRate(Long id, BigDecimal rate) {
        Shipment shipment = findOrThrow(id);
        shipment.setDutyRate(rate);
        shipmentRepository.save(shipment);

        List<Order> orders = orderRepository.findByShipmentId(id);
        for (Order order : orders) {
            order.setDutyRate(rate);
        }
        orderRepository.saveAll(orders);

        log.info("Taux mis à jour shipment {} → {}% ({} orders recalculés)",
                id, rate.multiply(new BigDecimal("100")), orders.size());
        return toResponse(shipment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Shipment shipment = findOrThrow(id);

        List<Order> orders = orderRepository.findByShipmentId(id);
        log.info("Suppression shipment {} ({}) — {} orders liés", id, shipment.getMawb(), orders.size());

        for (Order order : orders) {
            emailLogRepository.deleteByOrderId(order.getId());
            orderStatusHistoryRepository.deleteByOrderId(order.getId());
        }

        orderRepository.deleteAll(orders);
        shipmentRepository.delete(shipment);

        log.info("Shipment {} supprimé avec {} orders", shipment.getMawb(), orders.size());
    }

    private Shipment findOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment introuvable : " + id));
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) return u;
        return null;
    }

    private Map<Long, Long> countOrdersByShipmentIds(Page<Shipment> page) {
        List<Long> shipmentIds = page.getContent().stream()
                .map(Shipment::getId)
                .toList();
        if (shipmentIds.isEmpty()) {
            return Map.of();
        }
        return orderRepository.countByShipmentIds(shipmentIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private ShipmentResponseDTO toResponse(Shipment s) {
        return toResponse(s, s.getOrders().size());
    }

    private ShipmentResponseDTO toResponse(Shipment s, long totalOrders) {
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
                .exportDate(s.getExportDate())
                .importDate(s.getImportDate())
                .importingCarrier(s.getImportingCarrier())
                .modeOfTransport(s.getModeOfTransport())
                .portCode(s.getPortCode())
                .status(s.getStatus())
                .dutyRate(s.getDutyRate())
                .totalOrders((int) totalOrders)
                .shipper(shipperSummary)
                .createdBy(s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : "system")
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
