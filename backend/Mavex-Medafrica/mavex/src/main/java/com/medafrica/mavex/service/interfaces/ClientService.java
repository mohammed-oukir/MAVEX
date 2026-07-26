package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.client.BulkActionRequest;
import com.medafrica.mavex.dto.client.ClientPatchRequest;
import com.medafrica.mavex.dto.client.ClientRequestDTO;
import com.medafrica.mavex.dto.client.ClientResponseDTO;
import com.medafrica.mavex.dto.client.ClientSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientService {

    List<ClientResponseDTO> findAll();

    List<ClientResponseDTO> findAllActive();

    Page<ClientResponseDTO> search(ClientSearchCriteria criteria, Pageable pageable);

    ClientResponseDTO findById(Long id);

    ClientResponseDTO create(ClientRequestDTO dto);

    ClientResponseDTO update(Long id, ClientRequestDTO dto);

    ClientResponseDTO patch(Long id, ClientPatchRequest dto);

    void delete(Long id);

    int bulkDelete(BulkActionRequest req);

    int bulkActivate(BulkActionRequest req);

    int bulkDeactivate(BulkActionRequest req);
}
