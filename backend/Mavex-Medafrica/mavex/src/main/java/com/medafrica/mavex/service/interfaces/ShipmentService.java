package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.shipment.DutyChangeHistoryResponse;
import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.logistics.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ShipmentService {

    ShipmentResponseDTO create(ShipmentRequestDTO req);

    ShipmentResponseDTO getById(Long id);

    Page<ShipmentResponseDTO> list(Pageable pageable);

    Page<ShipmentResponseDTO> listByStatus(ShipmentStatus status, Pageable pageable);

    Page<ShipmentResponseDTO> search(
            String mawb,
            String shipperCompanyName,
            LocalDate importFrom,
            LocalDate importTo,
            String carrier,
            String mode,
            Integer totalOrders,
            Double dutyRateMin,
            Double dutyRateMax,
            ShipmentStatus status,
            Pageable pageable
    );

    /** Recherche avec filtres par colonne, sans pagination — renvoie tous les shipments filtrés (pour export). */
    List<Shipment> searchAll(
            String mawb,
            String shipperCompanyName,
            LocalDate importFrom,
            LocalDate importTo,
            String carrier,
            String mode,
            Integer totalOrders,
            Double dutyRateMin,
            Double dutyRateMax,
            ShipmentStatus status
    );

    /** Nombre d'orders par shipment id (pour export ou tout appelant hors pagination). */
    Map<Long, Long> countOrdersByShipmentIds(List<Long> shipmentIds);

    /** Récupère plusieurs shipments par leurs ids (pour export d'une sélection). */
    List<Shipment> findAllByIds(List<Long> ids);

    ShipmentResponseDTO update(Long id, ShipmentRequestDTO req);

    ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO req);

    ShipmentResponseDTO replace(Long id, ShipmentRequestDTO req);

    ShipmentResponseDTO updateDutyRate(Long id, BigDecimal rate);

    /** Historique des changements de duty rate faits directement sur ce Shipment. */
    Page<DutyChangeHistoryResponse> getShipmentDutyHistory(
            Long shipmentId, String changedByName, LocalDate from, LocalDate to, Pageable pageable);

    /** Historique des changements de duty rate faits sur les Orders de ce Shipment. */
    Page<DutyChangeHistoryResponse> getOrderDutyHistoryForShipment(
            Long shipmentId, String hawb, String changedByName, LocalDate from, LocalDate to, Pageable pageable);

    void delete(Long id);
}
