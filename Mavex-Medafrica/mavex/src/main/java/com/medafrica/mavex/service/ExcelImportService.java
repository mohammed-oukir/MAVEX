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
    private final CountryRepository      countryRepository;

    // ---------------------------------------------------------------
    // COLONNES EXCEL — index 0-based
    //  0  Mawb #
    //  1  Connote #
    //  2  Alternate Reference
    //  3  Sender Name
    //  4  Sender Country
    //  5  Sender Address 1
    //  6  Sender Location Name
    //  7  Sender State
    //  8  Sender Postcode
    //  9  Sender Contact
    // 10  Sender Phone
    // 11  Sender Email
    // 12  Receiver Name
    // 13  Receiver Country
    // 14  Receiver Address 1
    // 15  Receiver Location Name
    // 16  Receiver State
    // 17  Receiver Postcode
    // 18  Receiver Contact
    // 19  Receiver Phone
    // 20  Receiver Email
    // 21  Number of Items
    // 22  Goods Description
    // 23  Shipment Weight
    // 24  Customs Value
    // 25  Customs Currency Code
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

    // Taille max autorisée par ligne (champs texte)
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

    @Transactional
    public ImportLogResponse importManifest(MultipartFile file) throws Exception {

        // ── CAS 1 : Fichier vide
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou absent.");
        }

        // ── CAS 2 : Extension non autorisée
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("Format non supporté. Utilisez .xlsx ou .xls");
        }

        // ── CAS 3 : Taille fichier > 10 MB
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new IllegalArgumentException("Fichier trop volumineux. Maximum 10 MB autorisé.");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de lire le fichier : " + e.getMessage());
        }

        // ── CAS 4 : Fichier déjà importé (hash MD5)
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

            // ── CAS 5 : Feuille Excel vide
            if (sheet.getLastRowNum() < 1) {
                importLog.setStatus(ImportStatus.FAILED);
                importLog.setTotalRows(0);
                importLog.setSuccessRows(0);
                importLog.setSkippedRows(0);
                importLog.setFailedRows(0);
                importLogRepository.save(importLog);
                throw new IllegalArgumentException("Le fichier Excel est vide ou ne contient que l'en-tête.");
            }

            // ── CAS 6 : Vérifier que les en-têtes sont présents (ligne 0)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("La ligne d'en-tête est absente du fichier Excel.");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                // ── CAS 7 : Ligne physiquement null ou vide → ignorer silencieusement
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int rowNumber = i + 1;

                String mawb          = getCellString(row, COL_MAWB);
                String hawb          = getCellString(row, COL_HAWB);
                String receiverEmail = getCellString(row, COL_EMAIL);

                // ── Validation complète de la ligne
                String validationError = validateRow(row, mawb, hawb, receiverEmail);
                if (validationError != null) {
                    log.warn("Ligne {} rejetée — HAWB={} : {}", rowNumber, hawb, validationError);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, validationError));
                    failedRows++;
                    continue;
                }

                if (mawbFound == null) mawbFound = mawb;

                // ── CAS 8 : HAWB déjà en base (doublon DB)
                if (orderRepository.existsByHawb(hawb)) {
                    log.info("Ligne {} ignorée (doublon DB) — HAWB={}", rowNumber, hawb);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.SKIPPED, "HAWB déjà existant en base de données"));
                    skippedRows++;
                    continue;
                }

                // ── Traitement
                try {
                    Shipment shipment = findOrCreateShipment(mawb, currentUser);
                    Shipper shipper = findOrCreateShipper(row);

                    if (shipment.getShipper() == null) {
                        shipment.setShipper(shipper);
                        shipmentRepository.save(shipment);
                    }

                    Client client = findOrCreateClient(row);
                    createOrder(row, hawb, shipment, client);

                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.IMPORTED, null));
                    successRows++;
                    log.info("Ligne {} importée ✓ — HAWB={}", rowNumber, hawb);

                } catch (Exception e) {
                    log.error("Erreur ligne {} HAWB={} : {}", rowNumber, hawb, e.getMessage(), e);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, "Erreur traitement : " + e.getMessage()));
                    failedRows++;
                }
            }

        } catch (IllegalArgumentException e) {
            // Re-throw les erreurs de validation fichier
            throw e;
        } catch (Exception e) {
            // ── CAS 9 : Fichier Excel corrompu / illisible
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

        log.info("Import terminé — total={}, succès={}, ignorées={}, erreurs={}",
                totalRows, successRows, skippedRows, failedRows);

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

   private Shipper findOrCreateShipper(Row row) {
    String companyName = getCellString(row, COL_SENDER_NAME);
    if (companyName == null || companyName.isBlank()) companyName = "Unknown Shipper";
    final String name = companyName;

    // Résoudre le pays du shipper
    String senderCountryCode = getCellString(row, COL_SENDER_COUNTRY);
    Country senderCountry    = resolveCountry(senderCountryCode);

    // State : tronquer à 10 chars max
    String state = getCellString(row, COL_SENDER_STATE);
    if (state != null && state.length() > 10) state = state.substring(0, 10);
    final String finalState = state;

    return shipperRepository.findByCompanyNameIgnoreCase(name).map(existing -> {
        // ── Shipper existe → mettre à jour ses champs depuis le manifest
        existing.setContactName(getCellString(row, COL_SENDER_CONTACT));
        existing.setPhone(getCellString(row, COL_SENDER_PHONE));
        existing.setAddress(getCellString(row, COL_SENDER_ADDRESS));
        existing.setCity(getCellString(row, COL_SENDER_CITY));
        existing.setLocationName(getCellString(row, COL_SENDER_CITY));
        existing.setState(finalState);
        existing.setZipCode(getCellString(row, COL_SENDER_POSTCODE));
        if (senderCountry != null) existing.setCountry(senderCountry);

        String senderEmail = getCellString(row, COL_SENDER_EMAIL);
        if (senderEmail != null && !senderEmail.isBlank()) {
            existing.setEmail(senderEmail);
        }

        return shipperRepository.save(existing);

    }).orElseGet(() -> {
        // ── Shipper nouveau → créer
        Shipper s = Shipper.builder()
                .companyName(name)
                .contactName(getCellString(row, COL_SENDER_CONTACT))
                .email(
                    getCellString(row, COL_SENDER_EMAIL) != null
                        ? getCellString(row, COL_SENDER_EMAIL)
                        : name.toLowerCase().replaceAll("[^a-z0-9]", ".") + "@unknown.com"
                )
                .phone(getCellString(row, COL_SENDER_PHONE))
                .address(getCellString(row, COL_SENDER_ADDRESS))
                .city(getCellString(row, COL_SENDER_CITY))
                .locationName(getCellString(row, COL_SENDER_CITY))
                .state(finalState)
                .zipCode(getCellString(row, COL_SENDER_POSTCODE))
                .country(senderCountry)
                .build();

        return shipperRepository.save(s);
    });
}
 

    // ---------------------------------------------------------------
    // TROUVER OU CRÉER Client
    // ---------------------------------------------------------------

    private Client findOrCreateClient(Row row) {
    String email    = getCellString(row, COL_EMAIL);
    String fullName = getCellString(row, COL_RECEIVER_NAME);

    // Résoudre le pays UNE FOIS ici
    String countryCode = getCellString(row, COL_RECEIVER_COUNTRY);
    Country country    = resolveCountry(countryCode);

    // State : tronquer à 2 chars max
    String state = getCellString(row, COL_RECEIVER_STATE);
    if (state != null && state.length() > 2) state = state.substring(0, 2).toUpperCase();
    final String finalState = state;

    return clientRepository.findByEmail(email).map(existing -> {
        // ── Client existe → mettre à jour depuis le manifest
        existing.setFullName(fullName);
        existing.setPhone(getCellString(row, COL_RECEIVER_PHONE));
        existing.setAddress(getCellString(row, COL_RECEIVER_ADDRESS));
        existing.setCity(getCellString(row, COL_RECEIVER_CITY));
        existing.setState(finalState);
        existing.setZipCode(getCellString(row, COL_RECEIVER_POSTCODE));
        existing.setContactName(getCellString(row, COL_RECEIVER_CONTACT));
        if (country != null) existing.setCountry(country);

        return clientRepository.save(existing);

    }).orElseGet(() -> {
        // ── Client nouveau → créer
        Client c = Client.builder()
                .fullName(fullName)
                .email(email)
                .phone(getCellString(row, COL_RECEIVER_PHONE))
                .address(getCellString(row, COL_RECEIVER_ADDRESS))
                .city(getCellString(row, COL_RECEIVER_CITY))
                .state(finalState)
                .zipCode(getCellString(row, COL_RECEIVER_POSTCODE))
                .contactName(getCellString(row, COL_RECEIVER_CONTACT))
                .country(country)
                .build();
        return clientRepository.save(c);
    });
}

    // ---------------------------------------------------------------
    // CRÉER Order
    // ---------------------------------------------------------------

    private void createOrder(Row row, String hawb, Shipment shipment, Client client) {
        String currency = getCellString(row, COL_CURRENCY);

        // ── CAS currency manquante → USD par défaut
        if (currency == null || currency.isBlank()) {
            currency = "USD";
            log.warn("Currency manquante pour HAWB={} — défaut : USD", hawb);
        }

        // ── CAS currency invalide (pas 3 lettres) → USD par défaut
        if (!currency.matches("[A-Z]{3}")) {
            log.warn("Currency invalide '{}' pour HAWB={} — défaut : USD", currency, hawb);
            currency = "USD";
        }

        BigDecimal customs = getCellDecimal(row, COL_CUSTOMS_VALUE);
        BigDecimal weight  = getCellDecimal(row, COL_WEIGHT);

        Order order = Order.builder()
                .hawb(hawb)
                .alternateReference(getCellString(row, COL_ALTERNATE_REF))
                .goodsDescription(getCellString(row, COL_GOODS_DESC))
                .numberOfItems(getCellInteger(row, COL_NB_ITEMS))
                .shipmentWeight(weight)
                .customsValue(customs)
                .customsCurrency(currency)
                .shipment(shipment)
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        orderRepository.save(order);
    }

    // ---------------------------------------------------------------
    // RÉSOLUTION DU PAYS — souple
    // ---------------------------------------------------------------

    private Country resolveCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            log.warn("Code pays manquant — client importé sans pays");
            return null;
        }

        // Essai 1 : tel quel
        Country c = countryRepository.findById(countryCode.trim()).orElse(null);
        if (c != null) return c;

        // Essai 2 : majuscules
        c = countryRepository.findById(countryCode.trim().toUpperCase()).orElse(null);
        if (c != null) return c;

        // Essai 3 : recherche par nom complet
        c = countryRepository.findByNameIgnoreCase(countryCode.trim()).orElse(null);
        if (c != null) return c;

        log.warn("Pays '{}' introuvable — client importé sans pays lié", countryCode);
        return null;
    }

    // ---------------------------------------------------------------
    // VALIDATION COMPLÈTE D'UNE LIGNE
    // ---------------------------------------------------------------

    private String validateRow(Row row, String mawb, String hawb, String receiverEmail) {

        // ── CHAMPS OBLIGATOIRES ──────────────────────────────────────

        // CAS 10 : MAWB manquant
        if (mawb == null || mawb.isBlank())
            return "MAWB manquant";

        // CAS 11 : HAWB (Connote #) manquant
        if (hawb == null || hawb.isBlank())
            return "Connote # (HAWB) manquant";

        // CAS 12 : Receiver Name manquant
        String receiverName = getCellString(row, COL_RECEIVER_NAME);
        if (receiverName == null || receiverName.isBlank())
            return "Nom du destinataire manquant";

        // CAS 13 : Email manquant
        if (receiverEmail == null || receiverEmail.isBlank())
            return "Email du destinataire manquant";

        // CAS 14 : Customs Value manquante ou invalide
        BigDecimal customs = getCellDecimal(row, COL_CUSTOMS_VALUE);
        if (customs == null)
            return "Valeur douanière manquante";
        if (customs.compareTo(BigDecimal.ZERO) <= 0)
            return "Valeur douanière doit être > 0 (valeur actuelle : " + customs + ")";

        // ── VALIDATIONS FORMAT ───────────────────────────────────────

        // CAS 15 : Format email invalide
        if (!receiverEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return "Format email invalide : " + receiverEmail;

        // CAS 16 : Email trop long
        if (receiverEmail.length() > MAX_EMAIL_LENGTH)
            return "Email trop long (" + receiverEmail.length() + " chars, max " + MAX_EMAIL_LENGTH + ")";

        // CAS 17 : HAWB trop long
        if (hawb.length() > MAX_HAWB_LENGTH)
            return "HAWB trop long (" + hawb.length() + " chars, max " + MAX_HAWB_LENGTH + ")";

        // CAS 18 : MAWB trop long
        if (mawb.length() > MAX_MAWB_LENGTH)
            return "MAWB trop long (" + mawb.length() + " chars, max " + MAX_MAWB_LENGTH + ")";

        // CAS 19 : Receiver Name trop long
        if (receiverName.length() > MAX_NAME_LENGTH)
            return "Nom destinataire trop long (" + receiverName.length() + " chars, max " + MAX_NAME_LENGTH + ")";

        // ── VALIDATIONS NUMÉRIQUES ───────────────────────────────────

        // CAS 20 : Customs Value trop grande
        if (customs.compareTo(MAX_CUSTOMS_VALUE) > 0)
            return "Valeur douanière anormalement élevée : " + customs + " (max " + MAX_CUSTOMS_VALUE + ")";

        // CAS 21 : Poids manquant
        BigDecimal weight = getCellDecimal(row, COL_WEIGHT);
        if (weight == null)
            return "Poids du colis manquant";

        // CAS 22 : Poids négatif ou nul
        if (weight.compareTo(BigDecimal.ZERO) <= 0)
            return "Poids doit être > 0 (valeur actuelle : " + weight + ")";

        // CAS 23 : Poids trop grand
        if (weight.compareTo(MAX_WEIGHT) > 0)
            return "Poids anormalement élevé : " + weight + " kg (max " + MAX_WEIGHT + ")";

        // CAS 24 : Number of Items négatif ou nul
        Integer nbItems = getCellInteger(row, COL_NB_ITEMS);
        if (nbItems != null && nbItems <= 0)
            return "Nombre d'articles doit être > 0 (valeur actuelle : " + nbItems + ")";

        // CAS 25 : Number of Items anormalement grand
        if (nbItems != null && nbItems > 10000)
            return "Nombre d'articles anormalement élevé : " + nbItems;

        // CAS 26 : Description trop longue
        String description = getCellString(row, COL_GOODS_DESC);
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH)
            return "Description trop longue (" + description.length() + " chars, max " + MAX_DESCRIPTION_LENGTH + ")";

        // CAS 27 : Sender Name manquant → WARNING seulement (pas bloquant)
        // On laisse passer avec "Unknown Shipper" dans findOrCreateShipper()

        // CAS 28 : Currency présente mais format invalide → WARNING seulement
        // On corrige dans createOrder() avec valeur par défaut USD

        // CAS 29 : Code pays manquant ou invalide → WARNING seulement
        // On laisse passer avec country = null dans resolveCountry()

        // CAS 30 : Receiver State présent mais > 2 chars → on tronque dans findOrCreateClient()

        return null; // ✅ Ligne valide
    }

    // ---------------------------------------------------------------
    // DÉTECTION LIGNE VIDE
    // ---------------------------------------------------------------

    private boolean isRowEmpty(Row row) {
        for (int col : new int[]{COL_MAWB, COL_HAWB, COL_EMAIL}) {
            String val = getCellString(row, col);
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
            log.warn("Valeur numérique invalide à la colonne {} : {}", col, e.getMessage());
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
        List<ImportLogResponse.RowDetail> rows = log.getRowLogs() != null
                ? log.getRowLogs().stream()
                    .map(r -> ImportLogResponse.RowDetail.builder()
                            .rowNumber(r.getRowNumber())
                            .hawb(r.getHawb())
                            .receiverEmail(r.getReceiverEmail())
                            .status(r.getStatus())
                            .reason(r.getReason())
                            .build())
                    .toList()
                : List.of();

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