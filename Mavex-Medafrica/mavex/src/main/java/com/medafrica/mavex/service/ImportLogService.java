package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.repository.ImportLogRepository;
import com.medafrica.mavex.repository.ImportRowLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportLogService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;

    // ---------------------------------------------------------------
    // Liste paginée de tous les imports
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ImportLogResponse> list(Pageable pageable) {
        return importLogRepository.findAll(pageable).map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // Détail d'un import avec toutes ses lignes
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public ImportLogResponse getById(Long id) {
        ImportLog log = importLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Import introuvable id=" + id));
        return toResponseWithRows(log);
    }

    // ---------------------------------------------------------------
    // Mapping — version résumée (pour la liste paginée)
    // pas de rows pour ne pas charger des milliers de lignes inutilement
    // ---------------------------------------------------------------

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
                .rows(List.of())   // vide dans la liste — détail disponible via GET /{id}
                .build();
    }

    // ---------------------------------------------------------------
    // Mapping — version complète avec toutes les lignes (pour GET /{id})
    // ---------------------------------------------------------------

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