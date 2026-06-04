package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.shipper.ShipperRequestDTO;
import com.medafrica.mavex.dto.shipper.ShipperResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ShipperService {

    ShipperResponseDTO create(ShipperRequestDTO req);

    ShipperResponseDTO getById(Long id);

    Page<ShipperResponseDTO> list(Pageable pageable);

    ShipperResponseDTO update(Long id, ShipperRequestDTO req);

    ShipperResponseDTO patch(Long id, ShipperRequestDTO req);

    void deactivate(Long id);

    void activate(Long id);

    void delete(Long id);

    int bulkDelete(List<Long> ids);

    int bulkActivate(List<Long> ids);

    int bulkDeactivate(List<Long> ids);
}
