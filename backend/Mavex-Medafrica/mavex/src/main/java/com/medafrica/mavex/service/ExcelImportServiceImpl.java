package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.imports.ImportConfirmRequest;
import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.dto.imports.ImportPreviewResponse;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.model.enums.ImportRowStatus;
import com.medafrica.mavex.model.enums.ImportStatus;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.model.imports.ImportRowLog;
import com.medafrica.mavex.model.logistics.Airline;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.*;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;
    private final ShipmentRepository     shipmentRepository;
    private final ShipperRepository      shipperRepository;
    private final ClientRepository       clientRepository;
    private final OrderRepository        orderRepository;
    private final CountryRepository      countryRepository;
    private final AirlineRepository      airlineRepository;

    // ---------------------------------------------------------------
    // INDEX LOGIQUES — constantes utilisées comme clés dans cols[]
    // ---------------------------------------------------------------
    private static final int COL_MAWB               = 0;
    private static final int COL_HAWB               = 1;
    private static final int COL_ALTERNATE_REF      = 2;
    private static final int COL_SENDER_NAME        = 3;
    private static final int COL_SENDER_COUNTRY     = 4;
    private static final int COL_SENDER_ADDRESS     = 5;
    private static final int COL_SENDER_CITY        = 6;
    private static final int COL_SENDER_STATE       = 7;
    private static final int COL_SENDER_POSTCODE    = 8;
    private static final int COL_SENDER_CONTACT     = 9;
    private static final int COL_SENDER_PHONE       = 10;
    private static final int COL_SENDER_EMAIL       = 11;
    private static final int COL_RECEIVER_NAME      = 12;
    private static final int COL_RECEIVER_COUNTRY   = 13;
    private static final int COL_RECEIVER_ADDRESS   = 14;
    private static final int COL_RECEIVER_CITY      = 15;
    private static final int COL_RECEIVER_STATE     = 16;
    private static final int COL_RECEIVER_POSTCODE  = 17;
    private static final int COL_RECEIVER_CONTACT   = 18;
    private static final int COL_RECEIVER_PHONE     = 19;
    private static final int COL_EMAIL              = 20;
    private static final int COL_NB_ITEMS           = 21;
    private static final int COL_GOODS_DESC         = 22;
    private static final int COL_WEIGHT             = 23;
    private static final int COL_CUSTOMS_VALUE      = 24;
    private static final int COL_CURRENCY           = 25;

    // Noms attendus dans la ligne d'en-tête, dans le même ordre que les constantes ci-dessus
    private static final String[] COL_HEADERS = {
        "Mawb #",                 // 0
        "Connote #",              // 1
        "Alternate Reference",    // 2
        "Sender Name",            // 3
        "Sender Country",         // 4
        "Sender Address 1",       // 5
        "Sender Location Name",   // 6
        "Sender State",           // 7
        "Sender Postcode",        // 8
        "Sender Contact",         // 9
        "Sender Phone",           // 10
        "Sender Email",           // 11
        "Receiver Name",          // 12
        "Receiver Country",       // 13
        "Receiver Address 1",     // 14
        "Receiver Location Name", // 15
        "Receiver State",         // 16
        "Receiver Postcode",      // 17
        "Receiver Contact",       // 18
        "Receiver Phone",         // 19
        "Receiver Email",         // 20
        "Number of Items",        // 21
        "Goods Description",      // 22
        "Shipment Weight",        // 23
        "Customs Value",          // 24
        "Customs Currency Code"   // 25
    };

    // Colonnes obligatoires : leur absence bloque tout l'import au niveau fichier
    private static final Set<Integer> REQUIRED_COLS = Set.of(
        COL_MAWB, COL_HAWB, COL_RECEIVER_NAME, COL_EMAIL, COL_CUSTOMS_VALUE, COL_WEIGHT
    );

    private static final int MAX_EMAIL_LENGTH        = 255;
    private static final int MAX_NAME_LENGTH         = 200;
    private static final int MAX_HAWB_LENGTH         = 100;
    private static final int MAX_MAWB_LENGTH         = 100;
    private static final int MAX_DESCRIPTION_LENGTH  = 500;
    private static final BigDecimal MAX_CUSTOMS_VALUE = new BigDecimal("9999999.99");
    private static final BigDecimal MAX_WEIGHT        = new BigDecimal("99999.999");

    // ---------------------------------------------------------------
    // POINT D'ENTRÉE
    // ---------------------------------------------------------------

    @Override
    @Transactional
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
        ImportLog importLog = ImportLog.builder()
                .fileName(filename)
                .fileHash(fileHash)
                .importedBy(currentUser)
                .build();
        importLog = importLogRepository.save(importLog);

        List<ImportRowLog> rowLogs = new ArrayList<>();
        int totalRows   = 0;
        int successRows = 0;
        int skippedRows = 0;
        int failedRows  = 0;
        String mawbFound = null;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() < 1) {
                importLog.setStatus(ImportStatus.FAILED);
                importLog.setTotalRows(0);
                importLog.setSuccessRows(0);
                importLog.setSkippedRows(0);
                importLog.setFailedRows(0);
                importLogRepository.save(importLog);
                throw new IllegalArgumentException("Le fichier Excel est vide ou ne contient que l'en-tête.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                importLog.setStatus(ImportStatus.FAILED);
                importLogRepository.save(importLog);
                throw new IllegalArgumentException("La ligne d'en-tête est absente du fichier Excel.");
            }

            // ── Résolution dynamique : on cherche chaque colonne par son nom dans l'en-tête ──
            int[] cols;
            try {
                cols = buildColMap(headerRow);
            } catch (IllegalArgumentException e) {
                importLog.setStatus(ImportStatus.FAILED);
                importLog.setTotalRows(0);
                importLogRepository.save(importLog);
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

                List<String> rowErrors = validateRow(row, mawb, hawb, receiverEmail, cols);
                if (!rowErrors.isEmpty()) {
                    String joined = String.join(" | ", rowErrors);
                    log.warn("Ligne {} rejetée — HAWB={} : {}", rowNumber, hawb, joined);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, joined, null));
                    failedRows++;
                    continue;
                }

                if (mawbFound == null) mawbFound = mawb;

                if (orderRepository.existsByHawbAndShipmentMawb(hawb, mawb)) {
                    log.info("Ligne {} ignorée (doublon MAWB+HAWB) — MAWB={} HAWB={}", rowNumber, mawb, hawb);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.SKIPPED,
                            "Doublon détecté : HAWB=" + hawb + " déjà importé sous le MAWB=" + mawb, null));
                    skippedRows++;
                    continue;
                }

                if (orderRepository.existsByHawb(hawb)) {
                    log.info("Ligne {} ignorée (HAWB sous autre MAWB) — HAWB={}", rowNumber, hawb);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.SKIPPED,
                            "HAWB=" + hawb + " déjà existant dans la base sous un autre MAWB", null));
                    skippedRows++;
                    continue;
                }

                try {
                    List<String> warnings = new ArrayList<>();

                    Shipment shipment = findOrCreateShipment(mawb, currentUser);
                    Shipper  shipper  = findOrCreateShipper(row, cols, warnings);

                    if (shipment.getShipper() == null) {
                        shipment.setShipper(shipper);
                        shipmentRepository.save(shipment);
                    }

                    Client client = findOrCreateClient(row, cols, warnings);
                    createOrder(row, hawb, shipment, client, cols, warnings);

                    String warningText = warnings.isEmpty() ? null : String.join(" | ", warnings);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.IMPORTED, null, warningText));
                    successRows++;
                    if (warningText != null)
                        log.info("Ligne {} importée avec avertissements — HAWB={} : {}", rowNumber, hawb, warningText);
                    else
                        log.info("Ligne {} importée ✓ — HAWB={}", rowNumber, hawb);

                } catch (Exception e) {
                    log.error("Erreur ligne {} HAWB={} : {}", rowNumber, hawb, e.getMessage(), e);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, "Erreur traitement : " + e.getMessage(), null));
                    failedRows++;
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lecture fichier Excel '{}' : {}", filename, e.getMessage(), e);
            importLog.setStatus(ImportStatus.FAILED);
            importLog.setTotalRows(0);
            importLogRepository.save(importLog);
            throw new IllegalArgumentException("Fichier Excel illisible ou corrompu : " + e.getMessage());
        }

        importRowLogRepository.saveAll(rowLogs);

        importLog.setMawb(mawbFound);
        importLog.setTotalRows(totalRows);
        importLog.setSuccessRows(successRows);
        importLog.setSkippedRows(skippedRows);
        importLog.setFailedRows(failedRows);
        importLog.setStatus(resolveStatus(successRows, failedRows, skippedRows, totalRows));
        importLog.setRowLogs(rowLogs);
        importLogRepository.save(importLog);

        if (successRows > 0 && mawbFound != null) {
            shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(mawbFound).ifPresent(s -> {
                s.setStatus(com.medafrica.mavex.model.enums.ShipmentStatus.IMPORTED);
                shipmentRepository.save(s);
            });
        }

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

        // P2.1 — détecter si ce fichier exact a déjà été importé
        boolean alreadyImported = false;
        java.time.LocalDateTime previousImportDate = null;
        Optional<ImportLog> existing = importLogRepository.findByFileHash(fileHash);
        if (existing.isPresent()) {
            alreadyImported      = true;
            previousImportDate   = existing.get().getImportedAt();
        }

        List<ImportPreviewResponse.PreviewRow> previewRows = new ArrayList<>();
        int totalRows = 0, validRows = 0, invalidRows = 0, skippedRows = 0;
        String mawbFound = null;

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

                String mawb         = getCellString(row, cols[COL_MAWB]);
                String hawb         = getCellString(row, cols[COL_HAWB]);
                String receiverEmail = getCellString(row, cols[COL_EMAIL]);

                if (mawbFound == null && mawb != null) mawbFound = mawb;

                // Construire la ligne preview avec tous ses champs
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
                        .receiverName(getCellString(row, cols[COL_RECEIVER_NAME]))
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

                // Validation sans écriture en base
                List<String> rowErrors = validateRow(row, mawb, hawb, receiverEmail, cols);
                if (!rowErrors.isEmpty()) {
                    pb.previewStatus("INVALID").errors(rowErrors).warnings(List.of()).alreadyExists(false);
                    invalidRows++;
                } else if (orderRepository.existsByHawbAndShipmentMawb(hawb, mawb)
                        || orderRepository.existsByHawb(hawb)) {
                    pb.previewStatus("SKIPPED").errors(List.of("Déjà importé dans la base")).warnings(List.of()).alreadyExists(true);
                    skippedRows++;
                } else {
                    // P2.2 — calculer les corrections automatiques qui seront appliquées
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

    @Override
    @Transactional
    public ImportLogResponse confirmImport(ImportConfirmRequest request) throws Exception {

        User currentUser = getCurrentUser();

        // On génère un hash unique pour ce confirm (le fileHash original peut déjà exister)
        String logHash = request.getFileHash() + "_confirmed_" + System.currentTimeMillis();

        ImportLog importLog = ImportLog.builder()
                .fileName(request.getFileName())
                .fileHash(logHash)
                .importedBy(currentUser)
                .build();
        importLog = importLogRepository.save(importLog);

        List<ImportRowLog> rowLogs = new ArrayList<>();
        int totalRows = request.getRows().size();
        int successRows = 0, skippedRows = 0, failedRows = 0;
        String mawbFound = null;

        for (ImportConfirmRequest.RowData d : request.getRows()) {
            String hawb  = d.getHawb();
            String mawb  = d.getMawb();
            String email = d.getReceiverEmail();

            // Re-validation côté serveur (sécurité : le frontend peut envoyer n'importe quoi)
            List<String> rowErrors = validateRowData(d);
            if (!rowErrors.isEmpty()) {
                rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                        ImportRowStatus.FAILED, String.join(" | ", rowErrors), null));
                failedRows++;
                continue;
            }

            if (mawbFound == null) mawbFound = mawb;

            // Vérifier doublons
            if (orderRepository.existsByHawbAndShipmentMawb(hawb, mawb)) {
                rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                        ImportRowStatus.SKIPPED, "Doublon MAWB+HAWB", null));
                skippedRows++;
                continue;
            }
            if (orderRepository.existsByHawb(hawb)) {
                rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                        ImportRowStatus.SKIPPED, "HAWB existant sous autre MAWB", null));
                skippedRows++;
                continue;
            }

            try {
                List<String> warnings = new ArrayList<>();
                Shipment shipment = findOrCreateShipment(mawb, currentUser);
                Shipper  shipper  = findOrCreateShipperFromRequest(d, warnings);
                if (shipment.getShipper() == null) {
                    shipment.setShipper(shipper);
                    shipmentRepository.save(shipment);
                }
                Client client = findOrCreateClientFromRequest(d, warnings);
                createOrderFromRequest(d, shipment, client, warnings);

                String warningText = warnings.isEmpty() ? null : String.join(" | ", warnings);
                rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                        ImportRowStatus.IMPORTED, null, warningText));
                successRows++;
            } catch (Exception e) {
                log.error("Erreur confirm ligne {} HAWB={} : {}", d.getRowNumber(), hawb, e.getMessage(), e);
                rowLogs.add(buildRowLog(importLog, d.getRowNumber(), hawb, email,
                        ImportRowStatus.FAILED, "Erreur traitement : " + e.getMessage(), null));
                failedRows++;
            }
        }

        importRowLogRepository.saveAll(rowLogs);
        importLog.setMawb(mawbFound);
        importLog.setTotalRows(totalRows);
        importLog.setSuccessRows(successRows);
        importLog.setSkippedRows(skippedRows);
        importLog.setFailedRows(failedRows);
        importLog.setStatus(resolveStatus(successRows, failedRows, skippedRows, totalRows));
        importLog.setRowLogs(rowLogs);
        importLogRepository.save(importLog);

        if (successRows > 0 && mawbFound != null) {
            shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(mawbFound).ifPresent(s -> {
                s.setStatus(com.medafrica.mavex.model.enums.ShipmentStatus.IMPORTED);
                shipmentRepository.save(s);
            });
        }

        return buildResponse(importLog);
    }

    // ── Validation depuis le DTO (même règles que validateRow) ──────────

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

    // ── Shipper depuis RowData ───────────────────────────────────────────

    private Shipper findOrCreateShipperFromRequest(ImportConfirmRequest.RowData d, List<String> warnings) {
        String name = d.getSenderName();
        if (name == null || name.isBlank()) {
            name = "Unknown Shipper";
            warnings.add("Sender name manquant → 'Unknown Shipper'");
        }
        final String finalName = name;

        Country country = resolveCountry(d.getSenderCountry());

        String state = d.getSenderState();
        if (state != null && state.length() > 10) {
            state = state.substring(0, 10);
            warnings.add("Sender state tronqué à 10 chars");
        }
        final String finalState = state;

        return shipperRepository.findByCompanyNameIgnoreCase(finalName).map(existing -> {
            if (!existing.isActive()) {
                throw new IllegalArgumentException("Le shipper '" + existing.getCompanyName() + "' est désactivé. Veuillez le réactiver avant d'importer.");
            }
            existing.setContactName(d.getSenderContact());
            existing.setPhone(d.getSenderPhone());
            existing.setAddress(d.getSenderAddress());
            existing.setCity(d.getSenderCity());
            existing.setLocationName(d.getSenderCity());
            existing.setState(finalState);
            existing.setZipCode(d.getSenderPostcode());
            if (country != null) existing.setCountry(country);
            String email = d.getSenderEmail();
            if (email != null && !email.isBlank()) existing.setEmail(email);
            return shipperRepository.save(existing);
        }).orElseGet(() -> {
            String email = d.getSenderEmail();
            if (email == null || email.isBlank()) {
                email = finalName.toLowerCase().replaceAll("[^a-z0-9]", ".") + "@unknown.com";
                warnings.add("Sender email manquant → email généré");
            }
            final String finalEmail = email;
            return shipperRepository.save(Shipper.builder()
                    .companyName(finalName).contactName(d.getSenderContact())
                    .email(finalEmail).phone(d.getSenderPhone())
                    .address(d.getSenderAddress()).city(d.getSenderCity())
                    .locationName(d.getSenderCity()).state(finalState)
                    .zipCode(d.getSenderPostcode()).country(country).build());
        });
    }

    // ── Client depuis RowData ────────────────────────────────────────────

    private Client findOrCreateClientFromRequest(ImportConfirmRequest.RowData d, List<String> warnings) {
        String email    = d.getReceiverEmail();
        String fullName = d.getReceiverName();
        Country country = resolveCountry(d.getReceiverCountry());

        String state = d.getReceiverState();
        if (state != null && state.length() > 2) {
            state = state.substring(0, 2).toUpperCase();
            warnings.add("Receiver state tronqué à 2 chars");
        }
        final String finalState = state;

        return clientRepository.findByEmail(email).map(existing -> {
            if (!existing.isActive()) {
                throw new IllegalArgumentException("Le client '" + existing.getEmail() + "' est désactivé. Veuillez le réactiver avant d'importer.");
            }
            existing.setFullName(fullName);
            existing.setPhone(d.getReceiverPhone());
            existing.setAddress(d.getReceiverAddress());
            existing.setCity(d.getReceiverCity());
            existing.setState(finalState);
            existing.setZipCode(d.getReceiverPostcode());
            existing.setContactName(d.getReceiverContact());
            if (country != null) existing.setCountry(country);
            return clientRepository.save(existing);
        }).orElseGet(() -> clientRepository.save(Client.builder()
                .fullName(fullName).email(email).phone(d.getReceiverPhone())
                .address(d.getReceiverAddress()).city(d.getReceiverCity())
                .state(finalState).zipCode(d.getReceiverPostcode())
                .contactName(d.getReceiverContact()).country(country).build()));
    }

    // ── Order depuis RowData ─────────────────────────────────────────────

    private void createOrderFromRequest(ImportConfirmRequest.RowData d, Shipment shipment, Client client, List<String> warnings) {
        String currency = d.getCustomsCurrency();
        if (currency == null || currency.isBlank()) {
            currency = "USD"; warnings.add("Currency manquante → 'USD'");
        } else if (!currency.matches("[A-Z]{3}")) {
            currency = "USD"; warnings.add("Currency invalide → 'USD'");
        }
        orderRepository.save(Order.builder()
                .hawb(d.getHawb())
                .alternateReference(d.getAlternateReference())
                .goodsDescription(d.getGoodsDescription())
                .numberOfItems(d.getNumberOfItems())
                .shipmentWeight(d.getShipmentWeight())
                .customsValue(d.getCustomsValue())
                .customsCurrency(currency)
                .dutyRate(shipment.getDutyRate())
                .shipment(shipment).client(client)
                .status(OrderStatus.CREATED).build());
    }

    // ---------------------------------------------------------------
    // GÉNÉRATION DU TEMPLATE EXCEL
    // ---------------------------------------------------------------

    @Override
    public byte[] generateTemplate() throws Exception {

        // Données d'exemple — 2 lignes pour montrer le format attendu
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

            // ── Style en-tête : fond orange MAVEX + texte blanc gras ──
            CellStyle headerStyle = workbook.createCellStyle();
            // XSSFColor prend les bytes R, G, B du orange #F97316
            headerStyle.setFillForegroundColor(
                new XSSFColor(new byte[]{ (byte)0xF9, (byte)0x73, (byte)0x16 }, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);

            // ── Style lignes d'exemple : fond gris très clair ──
            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(
                new XSSFColor(new byte[]{ (byte)0xF9, (byte)0xFA, (byte)0xFB }, null));
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Ligne 0 : en-têtes de colonnes ──
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Lignes 1 et 2 : données d'exemple ──
            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < examples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(examples[r][c]);
                    cell.setCellStyle(exampleStyle);
                }
            }

            // ── Auto-dimensionner chaque colonne selon son contenu ──
            for (int i = 0; i < COL_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // ── Écrire le workbook en mémoire et retourner les octets ──
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ---------------------------------------------------------------
    // RÉSOLUTION DYNAMIQUE DES COLONNES DEPUIS L'EN-TÊTE
    // ---------------------------------------------------------------

    /**
     * Lit la ligne d'en-tête et construit un tableau cols[] tel que :
     *   cols[COL_MAWB] = index réel de la colonne "Mawb #" dans le fichier
     *   cols[i] = -1 si la colonne est absente (autorisé pour les optionnelles)
     * Lève IllegalArgumentException si une colonne obligatoire est absente.
     */
    private int[] buildColMap(Row headerRow) {
        // Construire la map : nom_en_tête_minuscule → index_réel
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
    // TROUVER OU CRÉER Shipment
    // ---------------------------------------------------------------

    private Shipment findOrCreateShipment(String mawb, User createdBy) {
        return shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(mawb).orElseGet(() -> {
            Shipment.ShipmentBuilder builder = Shipment.builder()
                    .mawb(mawb)
                    .createdBy(createdBy);

            // Détecter la compagnie aérienne depuis le préfixe MAWB
            String clean = mawb.replaceAll("[^0-9]", "");
            if (clean.length() >= 3) {
                airlineRepository.findById(clean.substring(0, 3)).ifPresent(airline -> {
                    builder.importingCarrier(airline.getName());
                    builder.modeOfTransport(airline.getMode());
                    log.info("Compagnie détectée depuis MAWB {} → {} ({})", mawb, airline.getName(), airline.getMode());
                });
            }

            return shipmentRepository.save(builder.build());
        });
    }

    // ---------------------------------------------------------------
    // TROUVER OU CRÉER Shipper
    // ---------------------------------------------------------------

    private Shipper findOrCreateShipper(Row row, int[] cols, List<String> warnings) {
        String companyName = getCellString(row, cols[COL_SENDER_NAME]);
        if (companyName == null || companyName.isBlank()) {
            companyName = "Unknown Shipper";
            warnings.add("Sender name manquant → remplacé par 'Unknown Shipper'");
        }
        final String name = companyName;

        String senderCountryCode = getCellString(row, cols[COL_SENDER_COUNTRY]);
        Country senderCountry = resolveCountry(senderCountryCode);
        if (senderCountryCode != null && !senderCountryCode.isBlank() && senderCountry == null)
            warnings.add("Pays expéditeur '" + senderCountryCode + "' introuvable → ignoré");

        String rawState = getCellString(row, cols[COL_SENDER_STATE]);
        String state = rawState;
        if (state != null && state.length() > 10) {
            state = state.substring(0, 10);
            warnings.add("Sender state tronqué à 10 chars : '" + rawState + "' → '" + state + "'");
        }
        final String finalState = state;

        return shipperRepository.findByCompanyNameIgnoreCase(name).map(existing -> {
            if (!existing.isActive()) {
                throw new IllegalArgumentException("Le shipper '" + existing.getCompanyName() + "' est désactivé. Veuillez le réactiver avant d'importer.");
            }
            existing.setContactName(getCellString(row, cols[COL_SENDER_CONTACT]));
            existing.setPhone(getCellString(row, cols[COL_SENDER_PHONE]));
            existing.setAddress(getCellString(row, cols[COL_SENDER_ADDRESS]));
            existing.setCity(getCellString(row, cols[COL_SENDER_CITY]));
            existing.setLocationName(getCellString(row, cols[COL_SENDER_CITY]));
            existing.setState(finalState);
            existing.setZipCode(getCellString(row, cols[COL_SENDER_POSTCODE]));
            if (senderCountry != null) existing.setCountry(senderCountry);

            String senderEmail = getCellString(row, cols[COL_SENDER_EMAIL]);
            if (senderEmail != null && !senderEmail.isBlank()) existing.setEmail(senderEmail);

            return shipperRepository.save(existing);

        }).orElseGet(() -> {
            String email = getCellString(row, cols[COL_SENDER_EMAIL]);
            if (email == null || email.isBlank()) {
                email = name.toLowerCase().replaceAll("[^a-z0-9]", ".") + "@unknown.com";
                warnings.add("Sender email manquant → email généré : '" + email + "'");
            }
            final String resolvedEmail = email;
            Shipper s = Shipper.builder()
                    .companyName(name)
                    .contactName(getCellString(row, cols[COL_SENDER_CONTACT]))
                    .email(resolvedEmail)
                    .phone(getCellString(row, cols[COL_SENDER_PHONE]))
                    .address(getCellString(row, cols[COL_SENDER_ADDRESS]))
                    .city(getCellString(row, cols[COL_SENDER_CITY]))
                    .locationName(getCellString(row, cols[COL_SENDER_CITY]))
                    .state(finalState)
                    .zipCode(getCellString(row, cols[COL_SENDER_POSTCODE]))
                    .country(senderCountry)
                    .build();
            return shipperRepository.save(s);
        });
    }

    // ---------------------------------------------------------------
    // TROUVER OU CRÉER Client
    // ---------------------------------------------------------------

    private Client findOrCreateClient(Row row, int[] cols, List<String> warnings) {
        String email    = getCellString(row, cols[COL_EMAIL]);
        String fullName = getCellString(row, cols[COL_RECEIVER_NAME]);

        String receiverCountryCode = getCellString(row, cols[COL_RECEIVER_COUNTRY]);
        Country country = resolveCountry(receiverCountryCode);
        if (receiverCountryCode != null && !receiverCountryCode.isBlank() && country == null)
            warnings.add("Pays destinataire '" + receiverCountryCode + "' introuvable → ignoré");

        String rawState = getCellString(row, cols[COL_RECEIVER_STATE]);
        String state = rawState;
        if (state != null && state.length() > 2) {
            state = state.substring(0, 2).toUpperCase();
            warnings.add("Receiver state tronqué à 2 chars : '" + rawState + "' → '" + state + "'");
        }
        final String finalState = state;

        return clientRepository.findByEmail(email).map(existing -> {
            if (!existing.isActive()) {
                throw new IllegalArgumentException("Le client '" + existing.getEmail() + "' est désactivé. Veuillez le réactiver avant d'importer.");
            }
            existing.setFullName(fullName);
            existing.setPhone(getCellString(row, cols[COL_RECEIVER_PHONE]));
            existing.setAddress(getCellString(row, cols[COL_RECEIVER_ADDRESS]));
            existing.setCity(getCellString(row, cols[COL_RECEIVER_CITY]));
            existing.setState(finalState);
            existing.setZipCode(getCellString(row, cols[COL_RECEIVER_POSTCODE]));
            existing.setContactName(getCellString(row, cols[COL_RECEIVER_CONTACT]));
            if (country != null) existing.setCountry(country);
            return clientRepository.save(existing);

        }).orElseGet(() -> {
            Client c = Client.builder()
                    .fullName(fullName)
                    .email(email)
                    .phone(getCellString(row, cols[COL_RECEIVER_PHONE]))
                    .address(getCellString(row, cols[COL_RECEIVER_ADDRESS]))
                    .city(getCellString(row, cols[COL_RECEIVER_CITY]))
                    .state(finalState)
                    .zipCode(getCellString(row, cols[COL_RECEIVER_POSTCODE]))
                    .contactName(getCellString(row, cols[COL_RECEIVER_CONTACT]))
                    .country(country)
                    .build();
            return clientRepository.save(c);
        });
    }

    // ---------------------------------------------------------------
    // CRÉER Order
    // ---------------------------------------------------------------

    private void createOrder(Row row, String hawb, Shipment shipment, Client client, int[] cols, List<String> warnings) {
        String currency = getCellString(row, cols[COL_CURRENCY]);

        if (currency == null || currency.isBlank()) {
            warnings.add("Currency manquante → forcée à 'USD'");
            currency = "USD";
        } else if (!currency.matches("[A-Z]{3}")) {
            warnings.add("Currency invalide '" + currency + "' → forcée à 'USD'");
            currency = "USD";
        }

        Order order = Order.builder()
                .hawb(hawb)
                .alternateReference(getCellString(row, cols[COL_ALTERNATE_REF]))
                .goodsDescription(getCellString(row, cols[COL_GOODS_DESC]))
                .numberOfItems(getCellInteger(row, cols[COL_NB_ITEMS]))
                .shipmentWeight(getCellDecimal(row, cols[COL_WEIGHT]))
                .customsValue(getCellDecimal(row, cols[COL_CUSTOMS_VALUE]))
                .customsCurrency(currency)
                .dutyRate(shipment.getDutyRate())
                .shipment(shipment)
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        orderRepository.save(order);
    }

    // ---------------------------------------------------------------
    // RÉSOLUTION DU PAYS
    // ---------------------------------------------------------------

    private Country resolveCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            log.warn("Code pays manquant — client importé sans pays");
            return null;
        }
        Country c = countryRepository.findById(countryCode.trim()).orElse(null);
        if (c != null) return c;
        c = countryRepository.findById(countryCode.trim().toUpperCase()).orElse(null);
        if (c != null) return c;
        c = countryRepository.findByNameIgnoreCase(countryCode.trim()).orElse(null);
        if (c != null) return c;
        log.warn("Pays '{}' introuvable — client importé sans pays lié", countryCode);
        return null;
    }

    // ---------------------------------------------------------------
    // VALIDATION COMPLÈTE D'UNE LIGNE
    // ---------------------------------------------------------------

    private List<String> validateRow(Row row, String mawb, String hawb, String receiverEmail, int[] cols) {
        List<String> errors = new ArrayList<>();

        // MAWB
        if (mawb == null || mawb.isBlank())       errors.add("MAWB manquant");
        else if (mawb.length() > MAX_MAWB_LENGTH) errors.add("MAWB trop long (" + mawb.length() + " chars, max " + MAX_MAWB_LENGTH + ")");

        // HAWB
        if (hawb == null || hawb.isBlank())       errors.add("Connote # (HAWB) manquant");
        else if (hawb.length() > MAX_HAWB_LENGTH) errors.add("HAWB trop long (" + hawb.length() + " chars, max " + MAX_HAWB_LENGTH + ")");

        // Nom destinataire
        String receiverName = getCellString(row, cols[COL_RECEIVER_NAME]);
        if (receiverName == null || receiverName.isBlank())       errors.add("Nom du destinataire manquant");
        else if (receiverName.length() > MAX_NAME_LENGTH)         errors.add("Nom destinataire trop long (" + receiverName.length() + " chars, max " + MAX_NAME_LENGTH + ")");

        // Email — on vérifie le format seulement si l'email est présent
        if (receiverEmail == null || receiverEmail.isBlank()) {
            errors.add("Email du destinataire manquant");
        } else {
            if (!receiverEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) errors.add("Format email invalide : " + receiverEmail);
            if (receiverEmail.length() > MAX_EMAIL_LENGTH)                 errors.add("Email trop long (" + receiverEmail.length() + " chars, max " + MAX_EMAIL_LENGTH + ")");
        }

        // Valeur douanière — on vérifie le montant seulement si la valeur existe
        BigDecimal customs = getCellDecimal(row, cols[COL_CUSTOMS_VALUE]);
        if (customs == null)                                  errors.add("Valeur douanière manquante");
        else if (customs.compareTo(BigDecimal.ZERO) <= 0)    errors.add("Valeur douanière doit être > 0 (valeur actuelle : " + customs + ")");
        else if (customs.compareTo(MAX_CUSTOMS_VALUE) > 0)   errors.add("Valeur douanière anormalement élevée : " + customs + " (max " + MAX_CUSTOMS_VALUE + ")");

        // Poids
        BigDecimal weight = getCellDecimal(row, cols[COL_WEIGHT]);
        if (weight == null)                               errors.add("Poids du colis manquant");
        else if (weight.compareTo(BigDecimal.ZERO) <= 0)  errors.add("Poids doit être > 0 (valeur actuelle : " + weight + ")");
        else if (weight.compareTo(MAX_WEIGHT) > 0)        errors.add("Poids anormalement élevé : " + weight + " kg (max " + MAX_WEIGHT + ")");

        // Nombre d'articles (optionnel — vérifié seulement si présent)
        Integer nbItems = getCellInteger(row, cols[COL_NB_ITEMS]);
        if (nbItems != null && nbItems <= 0)    errors.add("Nombre d'articles doit être > 0 (valeur actuelle : " + nbItems + ")");
        if (nbItems != null && nbItems > 10000) errors.add("Nombre d'articles anormalement élevé : " + nbItems);

        // Description (optionnelle)
        String description = getCellString(row, cols[COL_GOODS_DESC]);
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH)
            errors.add("Description trop longue (" + description.length() + " chars, max " + MAX_DESCRIPTION_LENGTH + ")");

        return errors;
    }

    // ---------------------------------------------------------------
    // P2.2 — CALCUL DES CORRECTIONS AUTOMATIQUES (warnings preview)
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
    // HELPERS — lecture cellules Excel
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

    private ImportLogResponse buildResponse(ImportLog log) {
        List<ImportLogResponse.RowDetail> rows = log.getRowLogs() != null
                ? log.getRowLogs().stream()
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
        if (log.getMawb() != null) {
            shipmentId = shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(log.getMawb())
                    .map(s -> s.getId())
                    .orElse(null);
        }

        return ImportLogResponse.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .mawb(log.getMawb())
                .shipmentId(shipmentId)
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
