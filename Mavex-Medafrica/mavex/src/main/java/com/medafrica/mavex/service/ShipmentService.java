package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.ShipmentRepository;
import com.medafrica.mavex.repository.ShipperRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipperRepository  shipperRepository;

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

    // ---------------------------------------------------------------
    // READ - détail
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public ShipmentResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ---------------------------------------------------------------
    // READ - liste paginée
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> list(Pageable pageable) {
        return shipmentRepository.findAll(pageable).map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // READ - liste par statut
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> listByStatus(ShipmentStatus status, Pageable pageable) {
        return shipmentRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // UPDATE - champs métier (PATCH, même DTO que create)
    // ---------------------------------------------------------------

    @Transactional
    public ShipmentResponseDTO update(Long id, ShipmentRequestDTO req) {

        Shipment shipment = findOrThrow(id);

        // MAWB : vérifier unicité seulement si on le change
        if (req.getMawb() != null && !req.getMawb().equalsIgnoreCase(shipment.getMawb())) {
            if (shipmentRepository.existsByMawb(req.getMawb())) {
                throw new IllegalArgumentException("MAWB déjà existant : " + req.getMawb());
            }
            shipment.setMawb(req.getMawb());
        }

        if (req.getExportDate()      != null) shipment.setExportDate(req.getExportDate());
        if (req.getImportDate()      != null) shipment.setImportDate(req.getImportDate());
        if (req.getImportingCarrier()!= null) shipment.setImportingCarrier(req.getImportingCarrier());
        if (req.getModeOfTransport() != null) shipment.setModeOfTransport(req.getModeOfTransport());
        if (req.getPortCode()        != null) shipment.setPortCode(req.getPortCode());

        if (req.getShipperId() != null) {
            Shipper shipper = shipperRepository.findById(req.getShipperId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
            shipment.setShipper(shipper);
        }

        return toResponse(shipmentRepository.save(shipment));
    }

    // ---------------------------------------------------------------
    // UPDATE - statut uniquement
    // ---------------------------------------------------------------

    @Transactional
    public ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO req) {
        Shipment shipment = findOrThrow(id);
        shipment.setStatus(req.getNewStatus());
        return toResponse(shipmentRepository.save(shipment));
    }



    // ---------------------------------------------------------------
// UPDATE - PUT (mise à jour complète, tous les champs obligatoires)
// ---------------------------------------------------------------

@Transactional
public ShipmentResponseDTO replace(Long id, ShipmentRequestDTO req) {

    Shipment shipment = findOrThrow(id);

    // Vérifier unicité MAWB uniquement si modifié
    if (!req.getMawb().equalsIgnoreCase(shipment.getMawb()) &&
         shipmentRepository.existsByMawb(req.getMawb())) {
        throw new IllegalArgumentException("MAWB déjà existant : " + req.getMawb());
    }

    Shipper shipper = null;
    if (req.getShipperId() != null) {
        shipper = shipperRepository.findById(req.getShipperId())
                .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + req.getShipperId()));
    }

    // On remplace TOUT
    shipment.setMawb(req.getMawb());
    shipment.setExportDate(req.getExportDate());
    shipment.setImportDate(req.getImportDate());
    shipment.setImportingCarrier(req.getImportingCarrier());
    shipment.setModeOfTransport(req.getModeOfTransport());
    shipment.setPortCode(req.getPortCode());
    shipment.setShipper(shipper);

    return toResponse(shipmentRepository.save(shipment));
}
    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Transactional
    public void delete(Long id) {
        shipmentRepository.delete(findOrThrow(id));
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
                .exportDate(s.getExportDate())
                .importDate(s.getImportDate())
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