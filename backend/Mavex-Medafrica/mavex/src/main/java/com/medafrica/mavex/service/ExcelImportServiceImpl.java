package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.imports.ImportConfirmRequest;
import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.dto.imports.ImportPreviewResponse;
import com.medafrica.mavex.model.enums.ImportRowStatus;
import com.medafrica.mavex.model.enums.ImportStatus;
import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.model.imports.ImportRowLog;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.*;
import com.medafrica.mavex.service.imports.ImportRowProcessor;
import com.medafrica.mavex.service.imports.RowOutcome;
import com.medafrica.mavex.service.interfaces.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;

import static com.medafrica.mavex.service.imports.ImportColumns.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;
    private final ShipmentRepository     shipmentRepository;
    private final OrderRepository        orderRepository;

    /** Traite chaque ligne dans SA propre transaction (REQUIRES_NEW). */
    private final ImportRowProcessor     rowProcessor;

    private static final int MAX_EMAIL_LENGTH        = 255;
    private static final int MAX_NAME_LENGTH         = 200;
    private static final int MAX_HAWB_LENGTH         = 100;
    private static final int MAX_MAWB_LENGTH         = 100;
    private static final int MAX_DESCRIPTION_LENGTH  = 500;
    private static final BigDecimal MAX_CUSTOMS_VALUE = new BigDecimal("9999999.99");
    private static final BigDecimal MAX_WEIGHT        = new BigDecimal("99999.999");

    // ---------------------------------------------------------------
    // POINT D'ENTRÉE — IMPORT DIRECT
    // ---------------------------------------------------------------
    //
    // NB : volontairement SANS @Transactional sur toute la méthode.
    // Chaque ligne est commitée/rollback indépendamment via rowProcessor
    // (REQUIRES_NEW). Une erreur DB sur une ligne n'annule plus les autres.

    @Override
    public ImportLogResponse importManifest(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Le fichier est vide ou absent.");

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls")))
            throw new IllegalArgumentException("Format non supporté. Utilisez .xlsx ou .xls");

        if (file.getSize() > 10L * 1024 * 1024)
            throw new IllegalArgumentException("Fichier trop volumineux. Maximum 10 MB autorisé.");

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de lire le fichier : " + e.getMessage());
        }

        String fileHash = computeMd5(fileBytes);
        if (importLogRepository.existsByFileHash(fileHash)) {
            ImportLog existing = importLogRepository.findByFileHash(fileHash).get();
            log.warn("Fichier déjà importé : {}", filename);
            return buildResponse(existing);
        }

        User currentUser = getCurrentUser();
        ImportLog importLog = importLogRepository.save(ImportLog.builder()
                .fileName(filename)
                .fileHash(fileHash)
                .importedBy(currentUser)
                .build());

        List<ImportRowLog> rowLogs = new ArrayList<>();
        int totalRows = 0, successRows = 0, skippedRows = 0, failedRows = 0;
        String mawbFound = null;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() < 1) {
                markFileFailed(importLog);
                throw new IllegalArgumentException("Le fichier Excel est vide ou ne contient que l'en-tête.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                markFileFailed(importLog);
                throw new IllegalArgumentException("La ligne d'en-tête est absente du fichier Excel.");
            }

            int[] cols;
            try {
                cols = buildColMap(headerRow);
            } catch (IllegalArgumentException e) {
                markFileFailed(importLog);
                throw e;
            }

            log.info("Mapping colonnes résolu pour '{}' — positions réelles : {}", filename, describeColMap(cols));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, cols)) continue;

                totalRows++;
                int rowNumber = i + 1;

                String mawb          = getCellString(row, cols[COL_MAWB]);
                String hawb          = getCellString(row, cols[COL_HAWB]);
                String receiverEmail = getCellString(row, cols[COL_EMAIL]);

                // 1. Validation (sans accès base) — rejet immédiat si invalide
                List<String> rowErrors = validateRow(row, mawb, hawb, receiverEmail, cols);
                if (!rowErrors.isEmpty()) {
                    String joined = String.join(" | ", rowErrors);
                    log.warn("Ligne {} rejetée — HAWB={} : {}", rowNumber, hawb, joined);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, joined, null));
                    failedRows++;
                    continue;
                }

                // 2. Traitement isolé dans sa propre transaction
                RowOutcome outcome;
                try {
                    outcome = rowProcessor.processExcelRow(row, mawb, hawb, cols, currentUser);
                } catch (Exception e) {
                    log.error("Erreur ligne {} HAWB={} : {}", rowNumber, hawb, e.getMessage());
                    outcome = RowOutcome.failed("Erreur traitement : " + rootMessage(e));
                }

                switch (outcome.status()) {
                    case IMPORTED -> {
                        if (mawbFound == null && outcome.mawb() != null) mawbFound = outcome.mawb();
                        successRows++;
                        if (outcome.warnings() != null)
                            log.info("Ligne {} importée avec avertissements — HAWB={} : {}", rowNumber, hawb, outcome.warnings());
                        else
                            log.info("Ligne {} importée ✓ — HAWB={}", rowNumber, hawb);
                    }
                    case SKIPPED -> {
                        skippedRows++;
                        log.info("Ligne {} ignorée — HAWB={} : {}", rowNumber, hawb, outcome.reason());
                    }
                    case FAILED  -> failedRows++;
                }

                rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                        outcome.status(), outcome.reason(), outcome.warnings()));
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lecture fichier Excel '{}' : {}", filename, e.getMessage(), e);
            markFileFailed(importLog);
            throw new IllegalArgumentException("Fichier Excel illisible ou corrompu : " + e.getMessage());
        }

        finalizeImport(importLog, rowLogs, totalRows, successRows, skippedRows, failedRows, mawbFound);

        log.info("Import terminé — total={}, succès={}, ignorées={}, erreurs={}",
                totalRows, successRows, skippedRows, failedRows);

        return buildResponse(importLog);
    }

    // ---------------------------------------------------------------
    // PREVIEW — lit + valide le fichier sans rien écrire en base
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ImportPreviewResponse previewManifest(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Le fichier est vide ou absent.");

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls")))
            throw new IllegalArgumentException("Format non supporté. Utilisez .xlsx ou .xls");

        if (file.getSize() > 10L * 1024 * 1024)
            throw new IllegalArgumentException("Fichier trop volumineux. Maximum 10 MB autorisé.");

        byte[] fileBytes = file.getBytes();
        String fileHash  = computeMd5(fileBytes);

        boolean alreadyImported = false;
        java.time.LocalDateTime previousImportDate = null;
        Optional<ImportLog> existing = importLogRepository.findByFileHash(fileHash);
        if (existing.isPresent()) {
            alreadyImported    = true;
            previousImportDate = existing.get().getImportedAt();
        }

        List<ImportPreviewResponse.PreviewRow> previewRows = new ArrayList<>();
        int totalRows = 0, validRows = 0, invalidRows = 0, skippedRows = 0;
        String mawbFound = null;

        // Détection des doublons internes au fichier (même HAWB sur 2 lignes)
        Set<String> seenHawb = new HashSet<>();
        // Détection des conflits email↔nom internes au fichier
        Map<String, String> emailToName = new HashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() < 1)
                throw new IllegalArgumentException("Le fichier Excel est vide ou ne contient que l'en-tête.");

            Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                throw new IllegalArgumentException("La ligne d'en-tête est absente.");

            int[] cols = buildColMap(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, cols)) continue;

                totalRows++;
                int rowNumber = i + 1;

                String mawb          = getCellString(row, cols[COL_MAWB]);
                String hawb          = getCellString(row, cols[COL_HAWB]);
                String receiverEmail = getCellString(row, cols[COL_EMAIL]);
                String receiverName  = getCellString(row, cols[COL_RECEIVER_NAME]);

                if (mawbFound == null && mawb != null) mawbFound = mawb;

                ImportPreviewResponse.PreviewRow.PreviewRowBuilder pb =
                        ImportPreviewResponse.PreviewRow.builder()
                        .rowNumber(rowNumber)
                        .mawb(mawb)
                        .hawb(hawb)
                        .alternateReference(getCellString(row, cols[COL_ALTERNATE_REF]))
                        .senderName(getCellString(row, cols[COL_SENDER_NAME]))
                        .senderCountry(getCellString(row, cols[COL_SENDER_COUNTRY]))
                        .senderAddress(getCellString(row, cols[COL_SENDER_ADDRESS]))
                        .senderCity(getCellString(row, cols[COL_SENDER_CITY]))
                        .senderState(getCellString(row, cols[COL_SENDER_STATE]))
                        .senderPostcode(getCellString(row, cols[COL_SENDER_POSTCODE]))
                        .senderContact(getCellString(row, cols[COL_SENDER_CONTACT]))
                        .senderPhone(getCellString(row, cols[COL_SENDER_PHONE]))
                        .senderEmail(getCellString(row, cols[COL_SENDER_EMAIL]))
                        .receiverName(receiverName)
                        .receiverCountry(getCellString(row, cols[COL_RECEIVER_COUNTRY]))
                        .receiverAddress(getCellString(row, cols[COL_RECEIVER_ADDRESS]))
                        .receiverCity(getCellString(row, cols[COL_RECEIVER_CITY]))
                        .receiverState(getCellString(row, cols[COL_RECEIVER_STATE]))
                        .receiverPostcode(getCellString(row, cols[COL_RECEIVER_POSTCODE]))
                        .receiverContact(getCellString(row, cols[COL_RECEIVER_CONTACT]))
                        .receiverPhone(getCellString(row, cols[COL_RECEIVER_PHONE]))
                        .receiverEmail(receiverEmail)
                        .numberOfItems(getCellInteger(row, cols[COL_NB_ITEMS]))
                        .goodsDescription(getCellString(row, cols[COL_GOODS_DESC]))
                        .shipmentWeight(getCellDecimal(row, cols[COL_WEIGHT]))
                        .customsValue(getCellDecimal(row, cols[COL_CUSTOMS_VALUE]))
                        .customsCurrency(getCellString(row, cols[COL_CURRENCY]));

                List<String> rowErrors = validateRow(row, mawb, hawb, receiverEmail, cols);

                // Doublon HAWB interne au fichier
                if (hawb != null && !seenHawb.add(hawb.trim().toLowerCase())) {
                    rowErrors = new ArrayList<>(rowErrors);
                    rowErrors.add("HAWB=" + hawb + " en doublon dans le fichier");
                }
                // Conflit email↔nom interne au fichier
                if (receiverEmail != null && receiverName != null && rowErrors.isEmpty()) {
                    String key = receiverEmail.trim().toLowerCase();
                    String prevName = emailToName.putIfAbsent(key, receiverName);
                    if (prevName != null && !prevName.equalsIgnoreCase(receiverName)) {
                        rowErrors = new ArrayList<>(rowErrors);
                        rowErrors.add("Email '" + receiverEmail + "' utilisé avec deux noms différents dans le fichier ('"
                                + prevName + "' et '" + receiverName + "')");
                    }
                }

                if (!rowErrors.isEmpty()) {
                    pb.previewStatus("INVALID").errors(rowErrors).warnings(List.of()).alreadyExists(false);
                    invalidRows++;
                } else if (orderRepository.existsByHawbAndShipmentMawb(hawb, mawb)
                        || orderRepository.existsByHawb(hawb)) {
                    pb.previewStatus("SKIPPED").errors(List.of("Déjà importé dans la base")).warnings(List.of()).alreadyExists(true);
                    skippedRows++;
                } else {
                    List<String> rowWarnings = computeRowWarnings(row, cols);
                    pb.previewStatus("VALID").errors(List.of()).warnings(rowWarnings).alreadyExists(false);
                    validRows++;
                }

                previewRows.add(pb.build());
            }
        }

        return ImportPreviewResponse.builder()
                .fileName(filename)
                .fileHash(fileHash)
                .mawb(mawbFound)
                .totalRows(totalRows)
                .validRows(validRows)
                .invalidRows(invalidRows)
                .skippedRows(skippedRows)
                .alreadyImported(alreadyImported)
                .previousImportDate(previousImportDate)
                .rows(previewRows)
                .build();
    }

    // ---------------------------------------------------------------
    // CONFIRM — crée les entités depuis les données corrigées du frontend
    // ---------------------------------------------------------------
    //
    // Comme importManifest : pas de @Transactional global, chaque ligne
    // est isolée via rowProcessor (REQUIRES_NEW).

    @Override
    public ImportLogResponse confirmImport(ImportConfirmRequest request) throws Exception {

        User currentUser = getCurrentUser();

        String logHash = request.getFileHash() + "_confirmed_" + System.currentTimeMillis();

        ImportLog importLog = importLogRepository.save(ImportLog.builder()
                .fileName(request.getFileName())
                .fileHash(logHash)
                .importedBy(currentUser)
                .build());

        List<ImportRowLog> rowLogs = new ArrayList<>();
        int totalRows = request.getRows().size();
        int successRows = 0, skippedRows = 0, failedRows = 0;
        String mawbFound = null;

        // Garde-fou : doublons HAWB internes à la requête
        Set<String> seenHawb = new HashSet<>();

        for (ImportConfirmRequest.RowData d : request.getRows()) {
            String hawb  = d.getHawb();
            String email = d.getReceiverEmail();

            // 1. Re-validation côté serveur
            List<String> rowErrors = validateRowData(d);
            if (hawb != null && !seenHawb.add(hawb.trim().toLowerCase())) {
                rowErrors.add("HAWB=" + hawb + " en doublon dans la requête");
            }
            if (!rowErrors.isEmpty()) {
                rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                        ImportRowStatus.FAILED, String.join(" | ", rowErrors), null));
                failedRows++;
                continue;
            }

            // 2. Traitement isolé
            RowOutcome outcome;
            try {
                outcome = rowProcessor.processConfirmRow(d, currentUser);
            } catch (Exception e) {
                log.error("Erreur confirm ligne {} HAWB={} : {}", d.getRowNumber(), hawb, e.getMessage());
                outcome = RowOutcome.failed("Erreur traitement : " + rootMessage(e));
            }

            switch (outcome.status()) {
                case IMPORTED -> { if (mawbFound == null && outcome.mawb() != null) mawbFound = outcome.mawb(); successRows++; }
                case SKIPPED  -> skippedRows++;
                case FAILED   -> failedRows++;
            }

            rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                    outcome.status(), outcome.reason(), outcome.warnings()));
        }

        finalizeImport(importLog, rowLogs, totalRows, successRows, skippedRows, failedRows, mawbFound);

        return buildResponse(importLog);
    }

    // ---------------------------------------------------------------
    // FINALISATION — persiste le log + ses lignes
    // ---------------------------------------------------------------
    //
    // Appelée en interne (this.), donc PAS de @Transactional ici (le proxy
    // Spring l'ignorerait). Chaque saveAll/save de Spring Data ouvre déjà
    // sa propre transaction, ce qui suffit : on persiste un état déjà calculé.

    private void finalizeImport(ImportLog importLog, List<ImportRowLog> rowLogs,
                                int total, int success, int skipped, int failed, String mawb) {
        importRowLogRepository.saveAll(rowLogs);

        importLog.setMawb(mawb);
        importLog.setTotalRows(total);
        importLog.setSuccessRows(success);
        importLog.setSkippedRows(skipped);
        importLog.setFailedRows(failed);
        importLog.setStatus(resolveStatus(success, failed, skipped, total));
        importLog.setRowLogs(rowLogs);
        importLogRepository.save(importLog);

        if (success > 0 && mawb != null) {
            shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(mawb).ifPresent(s -> {
                s.setStatus(com.medafrica.mavex.model.enums.ShipmentStatus.IMPORTED);
                shipmentRepository.save(s);
            });
        }
    }

    private void markFileFailed(ImportLog importLog) {
        importLog.setStatus(ImportStatus.FAILED);
        importLog.setTotalRows(0);
        importLog.setSuccessRows(0);
        importLog.setSkippedRows(0);
        importLog.setFailedRows(0);
        importLogRepository.save(importLog);
    }

    // ---------------------------------------------------------------
    // VALIDATION DEPUIS LE DTO (confirm)
    // ---------------------------------------------------------------

    private List<String> validateRowData(ImportConfirmRequest.RowData d) {
        List<String> errors = new ArrayList<>();
        if (d.getMawb()         == null || d.getMawb().isBlank())         errors.add("MAWB manquant");
        if (d.getHawb()         == null || d.getHawb().isBlank())         errors.add("HAWB manquant");
        if (d.getReceiverName() == null || d.getReceiverName().isBlank()) errors.add("Nom destinataire manquant");
        if (d.getReceiverEmail() == null || d.getReceiverEmail().isBlank()) {
            errors.add("Email destinataire manquant");
        } else if (!d.getReceiverEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.add("Format email invalide : " + d.getReceiverEmail());
        }
        if (d.getCustomsValue()   == null || d.getCustomsValue().compareTo(BigDecimal.ZERO)   <= 0) errors.add("Valeur douanière invalide");
        if (d.getShipmentWeight() == null || d.getShipmentWeight().compareTo(BigDecimal.ZERO) <= 0) errors.add("Poids invalide");
        return errors;
    }

    // ---------------------------------------------------------------
    // GÉNÉRATION DU TEMPLATE EXCEL
    // ---------------------------------------------------------------

    @Override
    public byte[] generateTemplate() throws Exception {

        String[][] examples = {
            { "12345678901", "HAWB001", "REF001",
              "DHL Express", "US", "1600 Pennsylvania Ave", "Washington", "DC", "20500",
              "John Doe", "+12025551234", "jdoe@dhl.com",
              "Mohammed Oukir", "MA", "123 Rue Hassan II", "Casablanca", "CA", "20000",
              "Mohammed", "+212600000000", "client@example.com",
              "2", "Electronic equipment", "5.5", "1200.00", "USD" },
            { "12345678901", "HAWB002", "REF002",
              "DHL Express", "US", "1600 Pennsylvania Ave", "Washington", "DC", "20500",
              "John Doe", "+12025551234", "jdoe@dhl.com",
              "Ahmed Benali", "MA", "456 Avenue Mohammed V", "Rabat", "RB", "10000",
              "Ahmed", "+212611111111", "ahmed@example.com",
              "1", "Clothing", "2.3", "350.00", "USD" }
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Manifeste");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(
                new XSSFColor(new byte[]{ (byte)0xF9, (byte)0x73, (byte)0x16 }, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);

            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(
                new XSSFColor(new byte[]{ (byte)0xF9, (byte)0xFA, (byte)0xFB }, null));
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < examples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(examples[r][c]);
                    cell.setCellStyle(exampleStyle);
                }
            }

            for (int i = 0; i < COL_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ---------------------------------------------------------------
    // RÉSOLUTION DYNAMIQUE DES COLONNES DEPUIS L'EN-TÊTE
    // ---------------------------------------------------------------

    private int[] buildColMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (int c = 0; c <= headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String text = null;
            if (cell.getCellType() == CellType.STRING) {
                text = cell.getStringCellValue().trim();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                text = String.valueOf((long) cell.getNumericCellValue()).trim();
            }
            if (text != null && !text.isEmpty()) {
                headerMap.put(text.toLowerCase(), c);
            }
        }

        int[] cols = new int[COL_HEADERS.length];
        List<String> missing = new ArrayList<>();

        for (int i = 0; i < COL_HEADERS.length; i++) {
            Integer found = headerMap.get(COL_HEADERS[i].toLowerCase());
            cols[i] = (found != null) ? found : -1;
            if (found == null && REQUIRED_COLS.contains(i)) {
                missing.add("\"" + COL_HEADERS[i] + "\"");
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "Structure du fichier invalide — colonnes obligatoires introuvables dans l'en-tête : "
                + String.join(", ", missing)
                + ". Vérifiez que vous utilisez le bon template Excel."
            );
        }

        return cols;
    }

    private String describeColMap(int[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(COL_HEADERS[i]).append("=").append(cols[i] < 0 ? "absent" : "col" + cols[i]);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // VALIDATION COMPLÈTE D'UNE LIGNE (import direct + preview)
    // ---------------------------------------------------------------

    private List<String> validateRow(Row row, String mawb, String hawb, String receiverEmail, int[] cols) {
        List<String> errors = new ArrayList<>();

        if (mawb == null || mawb.isBlank())       errors.add("MAWB manquant");
        else if (mawb.length() > MAX_MAWB_LENGTH) errors.add("MAWB trop long (" + mawb.length() + " chars, max " + MAX_MAWB_LENGTH + ")");

        if (hawb == null || hawb.isBlank())       errors.add("Connote # (HAWB) manquant");
        else if (hawb.length() > MAX_HAWB_LENGTH) errors.add("HAWB trop long (" + hawb.length() + " chars, max " + MAX_HAWB_LENGTH + ")");

        String receiverName = getCellString(row, cols[COL_RECEIVER_NAME]);
        if (receiverName == null || receiverName.isBlank())       errors.add("Nom du destinataire manquant");
        else if (receiverName.length() > MAX_NAME_LENGTH)         errors.add("Nom destinataire trop long (" + receiverName.length() + " chars, max " + MAX_NAME_LENGTH + ")");

        if (receiverEmail == null || receiverEmail.isBlank()) {
            errors.add("Email du destinataire manquant");
        } else {
            if (!receiverEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) errors.add("Format email invalide : " + receiverEmail);
            if (receiverEmail.length() > MAX_EMAIL_LENGTH)                 errors.add("Email trop long (" + receiverEmail.length() + " chars, max " + MAX_EMAIL_LENGTH + ")");
        }

        BigDecimal customs = getCellDecimal(row, cols[COL_CUSTOMS_VALUE]);
        if (customs == null)                                  errors.add("Valeur douanière manquante");
        else if (customs.compareTo(BigDecimal.ZERO) <= 0)    errors.add("Valeur douanière doit être > 0 (valeur actuelle : " + customs + ")");
        else if (customs.compareTo(MAX_CUSTOMS_VALUE) > 0)   errors.add("Valeur douanière anormalement élevée : " + customs + " (max " + MAX_CUSTOMS_VALUE + ")");

        BigDecimal weight = getCellDecimal(row, cols[COL_WEIGHT]);
        if (weight == null)                               errors.add("Poids du colis manquant");
        else if (weight.compareTo(BigDecimal.ZERO) <= 0)  errors.add("Poids doit être > 0 (valeur actuelle : " + weight + ")");
        else if (weight.compareTo(MAX_WEIGHT) > 0)        errors.add("Poids anormalement élevé : " + weight + " kg (max " + MAX_WEIGHT + ")");

        Integer nbItems = getCellInteger(row, cols[COL_NB_ITEMS]);
        if (nbItems != null && nbItems <= 0)    errors.add("Nombre d'articles doit être > 0 (valeur actuelle : " + nbItems + ")");
        if (nbItems != null && nbItems > 10000) errors.add("Nombre d'articles anormalement élevé : " + nbItems);

        String description = getCellString(row, cols[COL_GOODS_DESC]);
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH)
            errors.add("Description trop longue (" + description.length() + " chars, max " + MAX_DESCRIPTION_LENGTH + ")");

        return errors;
    }

    // ---------------------------------------------------------------
    // CALCUL DES CORRECTIONS AUTOMATIQUES (warnings preview)
    // ---------------------------------------------------------------

    private List<String> computeRowWarnings(Row row, int[] cols) {
        List<String> warnings = new ArrayList<>();

        String senderName = getCellString(row, cols[COL_SENDER_NAME]);
        if (senderName == null || senderName.isBlank())
            warnings.add("Sender name manquant → remplacé par 'Unknown Shipper'");

        String senderEmail = getCellString(row, cols[COL_SENDER_EMAIL]);
        if (senderEmail == null || senderEmail.isBlank())
            warnings.add("Sender email manquant → email généré automatiquement");

        String currency = getCellString(row, cols[COL_CURRENCY]);
        if (currency == null || currency.isBlank())
            warnings.add("Devise manquante → forcée à 'USD'");
        else if (!currency.matches("[A-Z]{3}"))
            warnings.add("Devise invalide '" + currency + "' → forcée à 'USD'");

        String senderState = getCellString(row, cols[COL_SENDER_STATE]);
        if (senderState != null && senderState.length() > 10)
            warnings.add("Sender State trop long → tronqué à 10 caractères");

        String receiverState = getCellString(row, cols[COL_RECEIVER_STATE]);
        if (receiverState != null && receiverState.length() > 2)
            warnings.add("Receiver State trop long → tronqué à 2 caractères");

        return warnings;
    }

    // ---------------------------------------------------------------
    // DÉTECTION LIGNE VIDE
    // ---------------------------------------------------------------

    private boolean isRowEmpty(Row row, int[] cols) {
        for (int logicalCol : new int[]{COL_MAWB, COL_HAWB, COL_EMAIL}) {
            int actual = cols[logicalCol];
            if (actual < 0) continue;
            String val = getCellString(row, actual);
            if (val != null && !val.isBlank()) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------
    // STATUT FINAL
    // ---------------------------------------------------------------

    private ImportStatus resolveStatus(int success, int failed, int skipped, int total) {
        if (total == 0)                       return ImportStatus.FAILED;
        if (success == total)                 return ImportStatus.SUCCESS;
        if (failed  == total)                 return ImportStatus.FAILED;
        if (success == 0 && skipped == total) return ImportStatus.SUCCESS;
        return ImportStatus.PARTIAL;
    }

    // ---------------------------------------------------------------
    // HELPERS — lecture cellules Excel (validation + preview)
    // ---------------------------------------------------------------

    private String getCellString(Row row, int col) {
        if (col < 0) return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> {
                String v = cell.getStringCellValue().trim();
                yield v.isEmpty() ? null : v;
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toString();
                double val = cell.getNumericCellValue();
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) {
                    try { yield String.valueOf((long) cell.getNumericCellValue()); }
                    catch (Exception ex) { yield null; }
                }
            }
            default -> null;
        };
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        if (col < 0) return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING  -> {
                    String s = cell.getStringCellValue().trim().replace(",", ".");
                    yield s.isEmpty() ? null : new BigDecimal(s);
                }
                case FORMULA -> {
                    try { yield BigDecimal.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e) { yield null; }
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.warn("Valeur numérique invalide colonne {} : {}", col, e.getMessage());
            return null;
        }
    }

    private Integer getCellInteger(Row row, int col) {
        BigDecimal val = getCellDecimal(row, col);
        return val != null ? val.intValue() : null;
    }

    // ---------------------------------------------------------------
    // HELPERS — utilitaires
    // ---------------------------------------------------------------

    private String computeMd5(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return HexFormat.of().formatHex(md.digest(data));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u : null;
    }

    /** Message le plus parlant d'une exception (utile quand l'erreur DB est imbriquée). */
    private String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : e.toString();
    }

    private ImportRowLog buildRowLog(ImportLog importLog, int rowNumber, String hawb,
                                     String email, ImportRowStatus status, String reason, String warnings) {
        return ImportRowLog.builder()
                .importLog(importLog)
                .rowNumber(rowNumber)
                .hawb(hawb)
                .receiverEmail(email)
                .status(status)
                .reason(reason)
                .warnings(warnings)
                .build();
    }

    // ---------------------------------------------------------------
    // MAPPING ImportLog → ImportLogResponse
    // ---------------------------------------------------------------

    private ImportLogResponse buildResponse(ImportLog logEntry) {
        List<ImportLogResponse.RowDetail> rows = logEntry.getRowLogs() != null
                ? logEntry.getRowLogs().stream()
                    .map(r -> ImportLogResponse.RowDetail.builder()
                            .rowNumber(r.getRowNumber())
                            .hawb(r.getHawb())
                            .receiverEmail(r.getReceiverEmail())
                            .status(r.getStatus())
                            .reason(r.getReason())
                            .warnings(r.getWarnings())
                            .build())
                    .toList()
                : List.of();

        Long shipmentId = null;
        if (logEntry.getMawb() != null) {
            shipmentId = shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(logEntry.getMawb())
                    .map(s -> s.getId())
                    .orElse(null);
        }

        return ImportLogResponse.builder()
                .id(logEntry.getId())
                .fileName(logEntry.getFileName())
                .mawb(logEntry.getMawb())
                .shipmentId(shipmentId)
                .totalRows(logEntry.getTotalRows())
                .successRows(logEntry.getSuccessRows())
                .skippedRows(logEntry.getSkippedRows())
                .failedRows(logEntry.getFailedRows())
                .status(logEntry.getStatus())
                .importedBy(logEntry.getImportedBy() != null ? logEntry.getImportedBy().getEmail() : "system")
                .importedAt(logEntry.getImportedAt())
                .rows(rows)
                .build();
    }
}
