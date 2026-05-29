package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ShipmentService {

    ShipmentResponseDTO create(ShipmentRequestDTO req);

    ShipmentResponseDTO getById(Long id);

    Page<ShipmentResponseDTO> list(Pageable pageable);

    Page<ShipmentResponseDTO> listByStatus(ShipmentStatus status, Pageable pageable);

    ShipmentResponseDTO update(Long id, ShipmentRequestDTO req);

    ShipmentResponseDTO updateStatus(Long id, ShipmentStatusUpdateDTO req);

    ShipmentResponseDTO replace(Long id, ShipmentRequestDTO req);

    ShipmentResponseDTO updateDutyRate(Long id, BigDecimal rate);

    void delete(Long id);
}
