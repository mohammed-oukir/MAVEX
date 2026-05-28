package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ImportLogService {

    Page<ImportLogResponse> list(Pageable pageable);

    ImportLogResponse getById(Long id);
}
