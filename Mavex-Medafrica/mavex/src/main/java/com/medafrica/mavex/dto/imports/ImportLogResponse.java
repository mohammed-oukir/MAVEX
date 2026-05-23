package com.medafrica.mavex.dto.imports;

import com.medafrica.mavex.model.enums.ImportRowStatus;
import com.medafrica.mavex.model.enums.ImportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ImportLogResponse {

    private Long id;
    private String fileName;
    private String mawb;
    private int totalRows;
    private int successRows;
    private int skippedRows;
    private int failedRows;
    private ImportStatus status;
    private String importedBy;
    private LocalDateTime importedAt;

    /** Détail ligne par ligne */
    private List<RowDetail> rows;

    @Data
    @Builder
    public static class RowDetail {
        private int rowNumber;
        private String hawb;
        private String receiverEmail;
        private ImportRowStatus status;
        private String reason;
    }
}