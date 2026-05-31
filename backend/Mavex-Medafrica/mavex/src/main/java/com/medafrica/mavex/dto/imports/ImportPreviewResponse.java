package com.medafrica.mavex.dto.imports;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ImportPreviewResponse {

    private String fileName;
    private String fileHash;
    private String mawb;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int skippedRows;

    /** P2.1 — ce fichier a déjà été importé (même MD5) */
    private boolean alreadyImported;
    private LocalDateTime previousImportDate;

    private List<PreviewRow> rows;

    @Data
    @Builder
    public static class PreviewRow {
        private int    rowNumber;
        private String previewStatus;   // "VALID" | "INVALID" | "SKIPPED"
        private List<String> errors;    // raisons d'invalidité
        private List<String> warnings;  // P2.2 — corrections automatiques qui seront appliquées
        private boolean alreadyExists;

        // ── Tous les champs éditables ──
        private String mawb;
        private String hawb;
        private String alternateReference;
        private String senderName;
        private String senderCountry;
        private String senderAddress;
        private String senderCity;
        private String senderState;
        private String senderPostcode;
        private String senderContact;
        private String senderPhone;
        private String senderEmail;
        private String receiverName;
        private String receiverCountry;
        private String receiverAddress;
        private String receiverCity;
        private String receiverState;
        private String receiverPostcode;
        private String receiverContact;
        private String receiverPhone;
        private String receiverEmail;
        private Integer    numberOfItems;
        private String     goodsDescription;
        private BigDecimal shipmentWeight;
        private BigDecimal customsValue;
        private String     customsCurrency;
    }
}
