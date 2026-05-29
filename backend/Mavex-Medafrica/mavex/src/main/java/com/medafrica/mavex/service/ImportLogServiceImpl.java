package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.repository.ImportLogRepository;
import com.medafrica.mavex.repository.ImportRowLogRepository;
import com.medafrica.mavex.service.interfaces.ImportLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportLogServiceImpl implements ImportLogService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ImportLogResponse> list(Pageable pageable) {
        return importLogRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportLogResponse getById(Long id) {
        ImportLog log = importLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Import introuvable id=" + id));
        return toResponseWithRows(log);
    }

    private ImportLogResponse toResponse(ImportLog log) {
        return ImportLogResponse.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .mawb(log.getMawb())
                .totalRows(log.getTotalRows())
                .successRows(log.getSuccessRows())
                .skippedRows(log.getSkippedRows())
                .failedRows(log.getFailedRows())
                .status(log.getStatus())
                .importedBy(log.getImportedBy() != null ? log.getImportedBy().getEmail() : "system")
                .importedAt(log.getImportedAt())
                .rows(List.of())
                .build();
    }

    private ImportLogResponse toResponseWithRows(ImportLog log) {
        List<ImportLogResponse.RowDetail> rows = importRowLogRepository
                .findByImportLog(log)
                .stream()
                .map(r -> ImportLogResponse.RowDetail.builder()
                        .rowNumber(r.getRowNumber())
                        .hawb(r.getHawb())
                        .receiverEmail(r.getReceiverEmail())
                        .status(r.getStatus())
                        .reason(r.getReason())
                        .build())
                .toList();

        return ImportLogResponse.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .mawb(log.getMawb())
                .totalRows(log.getTotalRows())
                .successRows(log.getSuccessRows())
                .skippedRows(log.getSkippedRows())
                .failedRows(log.getFailedRows())
                .status(log.getStatus())
                .importedBy(log.getImportedBy() != null ? log.getImportedBy().getEmail() : "system")
                .importedAt(log.getImportedAt())
                .rows(rows)
                .build();
    }
}
