package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.model.enums.ImportRowStatus;
import com.medafrica.mavex.model.enums.ImportStatus;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.model.imports.ImportRowLog;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.medafrica.mavex.repository.CountryRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final ImportLogRepository    importLogRepository;
    private final ImportRowLogRepository importRowLogRepository;
    private final ShipmentRepository     shipmentRepository;
    private final ShipperRepository      shipperRepository;
    private final ClientRepository       clientRepository;
    private final OrderRepository        orderRepository;
    private final CountryRepository      countryRepository;  // ← ajouté


    // Colonnes Excel (index 0-based)
    private static final int COL_MAWB             = 0;
    private static final int COL_HAWB             = 1;
    private static final int COL_SENDER_NAME      = 2;
    private static final int COL_RECEIVER_NAME    = 3;
    private static final int COL_RECEIVER_COUNTRY = 4;
    private static final int COL_ADDRESS          = 5;
    private static final int COL_CITY             = 6;
    private static final int COL_STATE            = 7;
    private static final int COL_POSTCODE         = 8;
    private static final int COL_PHONE            = 9;
    private static final int COL_EMAIL            = 10;
    private static final int COL_NB_ITEMS         = 11;
    private static final int COL_GOODS_DESC       = 12;
    private static final int COL_WEIGHT           = 13;
    private static final int COL_CUSTOMS_VALUE    = 14;
    private static final int COL_CURRENCY         = 15;

    // ---------------------------------------------------------------
    // POINT D'ENTRÉE
    // ---------------------------------------------------------------

    @Transactional
    public ImportLogResponse importManifest(MultipartFile file) throws Exception {

        // 1. Calcul du hash MD5 pour détecter les doublons de fichier
        byte[] fileBytes = file.getBytes();
        String fileHash  = computeMd5(fileBytes);

        // 2. Vérifier si ce fichier a déjà été importé
        if (importLogRepository.existsByFileHash(fileHash)) {
            ImportLog existing = importLogRepository.findByFileHash(fileHash).get();
            log.warn("Fichier déjà importé : {}", file.getOriginalFilename());
            return buildResponse(existing);
        }

        // 3. Créer l'ImportLog (status FAILED par défaut, mis à jour à la fin)
        User currentUser = getCurrentUser();
        ImportLog importLog = ImportLog.builder()
                .fileName(file.getOriginalFilename())
                .fileHash(fileHash)
                .importedBy(currentUser)
                .build();
        importLog = importLogRepository.save(importLog);

        // 4. Lire le fichier Excel et traiter chaque ligne
        List<ImportRowLog> rowLogs = new ArrayList<>();
        int totalRows   = 0;
        int successRows = 0;
        int skippedRows = 0;
        int failedRows  = 0;
        String mawbFound = null;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Ligne 0 = header → on commence à 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRows++;
                int rowNumber = i + 1; // numéro lisible pour l'agent (commence à 2)

                String hawb          = getCellString(row, COL_HAWB);
                String receiverEmail = getCellString(row, COL_EMAIL);

                // ── Validation des champs obligatoires ──
                String validationError = validateRow(row, hawb, receiverEmail);
                if (validationError != null) {
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, validationError));
                    failedRows++;
                    continue;
                }

                // ── Vérifier si le HAWB existe déjà ──
                if (orderRepository.existsByHawb(hawb)) {
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.SKIPPED, "HAWB already exists in database"));
                    skippedRows++;
                    continue;
                }

                // ── Traitement de la ligne ──
                try {
                    String mawb       = getCellString(row, COL_MAWB);
                    String senderName = getCellString(row, COL_SENDER_NAME);
                    if (mawbFound == null) mawbFound = mawb;

                    // Trouver ou créer le Shipment (MAWB)
                    Shipment shipment = findOrCreateShipment(mawb, currentUser);

                    // Trouver ou créer le Shipper (Sender Name)
                    Shipper shipper = findOrCreateShipper(senderName);

                    // Lier le shipper au shipment si pas encore fait
                    if (shipment.getShipper() == null) {
                        shipment.setShipper(shipper);
                        shipmentRepository.save(shipment);
                    }

                    // Trouver ou créer le Client (Receiver)
                    Client client = findOrCreateClient(row);

                    // Créer l'Order    
                    createOrder(row, hawb, shipment, client);

                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.IMPORTED, null));
                    successRows++;

                } catch (Exception e) {
                    log.error("Erreur ligne {} HAWB={} : {}", rowNumber, hawb, e.getMessage());
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, e.getMessage()));
                    failedRows++;
                }
            }
        }

        // 5. Sauvegarder tous les ImportRowLog
        importRowLogRepository.saveAll(rowLogs);

        // 6. Mettre à jour l'ImportLog avec les compteurs et le statut final
        importLog.setMawb(mawbFound);
        importLog.setTotalRows(totalRows);
        importLog.setSuccessRows(successRows);
        importLog.setSkippedRows(skippedRows);
        importLog.setFailedRows(failedRows);
        importLog.setStatus(resolveStatus(successRows, failedRows, skippedRows, totalRows));
        importLog.setRowLogs(rowLogs);
        importLogRepository.save(importLog);

        return buildResponse(importLog);
    }

    // ---------------------------------------------------------------
    // TROUVER OU CRÉER Shipment
    // ---------------------------------------------------------------

    private Shipment findOrCreateShipment(String mawb, User createdBy) {
        return shipmentRepository.findByMawb(mawb).orElseGet(() -> {
            Shipment s = Shipment.builder()
                    .mawb(mawb)
                    .createdBy(createdBy)
                    .build();
            return shipmentRepository.save(s);
        });
    }

    // ---------------------------------------------------------------
    // TROUVER OU CRÉER Shipper
    // ---------------------------------------------------------------

    private Shipper findOrCreateShipper(String companyName) {
        return shipperRepository.findByCompanyNameIgnoreCase(companyName).orElseGet(() -> {
            Shipper s = Shipper.builder()
                    .companyName(companyName)
                    .email(companyName.toLowerCase().replaceAll("\\s+", ".") + "@unknown.com")
                    .build();
            return shipperRepository.save(s);
        });
    }

    // ---------------------------------------------------------------
    // TROUVER OU CRÉER Client
    // ---------------------------------------------------------------

   // APRÈS — country résolue depuis findById
private Client findOrCreateClient(Row row) {
    String email       = getCellString(row, COL_EMAIL);
    String fullName    = getCellString(row, COL_RECEIVER_NAME);
    String countryCode = getCellString(row, COL_RECEIVER_COUNTRY); // "US"

    return clientRepository.findByEmail(email).orElseGet(() -> {

        Country country = countryRepository.findById(countryCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country introuvable pour le code : " + countryCode));

        Client c = Client.builder()
                .fullName(fullName)
                .email(email)
                .phone(getCellString(row, COL_PHONE))
                .address(getCellString(row, COL_ADDRESS))
                .city(getCellString(row, COL_CITY))
                .state(getCellString(row, COL_STATE))
                .zipCode(getCellString(row, COL_POSTCODE))
                .country(country)   // ← ajouté
                .build();
        return clientRepository.save(c);
    });
}

    // ---------------------------------------------------------------
    // CRÉER Order
    // ---------------------------------------------------------------

    private void createOrder(Row row, String hawb, Shipment shipment, Client client) {
        String currency = getCellString(row, COL_CURRENCY);
        BigDecimal customsValue = getCellDecimal(row, COL_CUSTOMS_VALUE);

        Order order = Order.builder()
                .hawb(hawb)
                .goodsDescription(getCellString(row, COL_GOODS_DESC))
                .numberOfItems(getCellInteger(row, COL_NB_ITEMS))
                .shipmentWeight(getCellDecimal(row, COL_WEIGHT))
                .customsValue(customsValue)
                .customsCurrency(currency != null ? currency : "USD")
                .shipment(shipment)
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        orderRepository.save(order);
    }

    // ---------------------------------------------------------------
    // VALIDATION d'une ligne
    // ---------------------------------------------------------------

    private String validateRow(Row row, String hawb, String email) {
        if (hawb == null || hawb.isBlank())
            return "Connote # (HAWB) is missing";
        if (email == null || email.isBlank())
            return "Receiver email is missing";
        if (getCellString(row, COL_MAWB) == null || getCellString(row, COL_MAWB).isBlank())
            return "MAWB is missing";
        if (getCellString(row, COL_RECEIVER_NAME) == null || getCellString(row, COL_RECEIVER_NAME).isBlank())
            return "Receiver name is missing";
        BigDecimal customsValue = getCellDecimal(row, COL_CUSTOMS_VALUE);
        if (customsValue == null || customsValue.compareTo(BigDecimal.ZERO) <= 0)
            return "Customs value is missing or invalid";
        return null; // OK
    }

    // ---------------------------------------------------------------
    // STATUT FINAL de l'import
    // ---------------------------------------------------------------

    private ImportStatus resolveStatus(int success, int failed, int skipped, int total) {
        if (success == total)                  return ImportStatus.SUCCESS;
        if (failed == total)                   return ImportStatus.FAILED;
        if (success == 0 && skipped == total)  return ImportStatus.SUCCESS; // tout skippé = déjà importé
        return ImportStatus.PARTIAL;
    }

    // ---------------------------------------------------------------
    // HELPERS — lecture des cellules Excel
    // ---------------------------------------------------------------

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getLocalDateTimeCellValue().toString();
                // Pour les HAWB/MAWB numériques → pas de notation scientifique
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING  -> new BigDecimal(cell.getStringCellValue().trim());
                default      -> null;
            };
        } catch (NumberFormatException e) {
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
        byte[] hash = md.digest(data);
        return HexFormat.of().formatHex(hash);
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u : null;
    }

    private ImportRowLog buildRowLog(ImportLog importLog, int rowNumber, String hawb,
                                     String email, ImportRowStatus status, String reason) {
        return ImportRowLog.builder()
                .importLog(importLog)
                .rowNumber(rowNumber)
                .hawb(hawb)
                .receiverEmail(email)
                .status(status)
                .reason(reason)
                .build();
    }

    // ---------------------------------------------------------------
    // MAPPING ImportLog → ImportLogResponse
    // ---------------------------------------------------------------

    private ImportLogResponse buildResponse(ImportLog log) {
        List<ImportLogResponse.RowDetail> rows = log.getRowLogs().stream()
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