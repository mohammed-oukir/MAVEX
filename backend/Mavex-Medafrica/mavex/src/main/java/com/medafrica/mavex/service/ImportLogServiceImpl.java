package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.dto.imports.ImportStatsResponse;
import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.repository.ImportLogRepository;
import com.medafrica.mavex.repository.ImportRowLogRepository;
import com.medafrica.mavex.repository.ShipmentRepository;
import com.medafrica.mavex.service.interfaces.ImportLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportLogServiceImpl implements ImportLogService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;
    private final ShipmentRepository     shipmentRepository;

    private static final int RECENT_WINDOW_DAYS = 30;

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

    @Override
    @Transactional(readOnly = true)
    public ImportStatsResponse getStats() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        LocalDateTime since30Days = LocalDateTime.now().minusDays(RECENT_WINDOW_DAYS);

        long importsThisMonth = importLogRepository.countByImportedAtBetween(startOfMonth, startOfNextMonth);
        long totalImportsRecent = importLogRepository.countByImportedAtAfter(since30Days);

        List<Object[]> sumsResult = importLogRepository.sumRowsSince(since30Days);
        Object[] sums = sumsResult.isEmpty() ? new Object[]{0L, 0L, 0L} : sumsResult.get(0);
        long totalRowsRecent   = ((Number) sums[0]).longValue();
        long successRowsRecent = ((Number) sums[1]).longValue();
        long failedRowsRecent  = ((Number) sums[2]).longValue();

        double successRatePercent = totalRowsRecent == 0
                ? 0.0
                : Math.round((successRowsRecent * 10000.0) / totalRowsRecent) / 100.0;

        return ImportStatsResponse.builder()
                .importsThisMonth(importsThisMonth)
                .successRatePercent(successRatePercent)
                .failedRowsRecent(failedRowsRecent)
                .totalImportsRecent(totalImportsRecent)
                .build();
    }

    private Long resolveShipmentId(String mawb) {
        if (mawb == null) return null;
        return shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(mawb)
                .map(s -> s.getId())
                .orElse(null);
    }

    private ImportLogResponse toResponse(ImportLog log) {
        return ImportLogResponse.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .mawb(log.getMawb())
                .shipmentId(resolveShipmentId(log.getMawb()))
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
                        .warnings(r.getWarnings())
                        .build())
                .toList();

        return ImportLogResponse.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .mawb(log.getMawb())
                .shipmentId(resolveShipmentId(log.getMawb()))
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
