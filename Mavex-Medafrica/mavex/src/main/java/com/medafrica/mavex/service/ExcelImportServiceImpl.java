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
import com.medafrica.mavex.service.interfaces.ExcelImportService;
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

                String validationError = validateRow(row, mawb, hawb, receiverEmail, cols);
                if (validationError != null) {
                    log.warn("Ligne {} rejetée — HAWB={} : {}", rowNumber, hawb, validationError);
                    rowLogs.add(buildRowLog(importLog, rowNumber, hawb, receiverEmail,
                            ImportRowStatus.FAILED, validationError, null));
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

        log.info("Import terminé — total={}, succès={}, ignorées={}, erreurs={}",
                totalRows, successRows, skippedRows, failedRows);

        return buildResponse(importLog);
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

    private String validateRow(Row row, String mawb, String hawb, String receiverEmail, int[] cols) {

        if (mawb == null || mawb.isBlank())
            return "MAWB manquant";

        if (hawb == null || hawb.isBlank())
            return "Connote # (HAWB) manquant";

        String receiverName = getCellString(row, cols[COL_RECEIVER_NAME]);
        if (receiverName == null || receiverName.isBlank())
            return "Nom du destinataire manquant";

        if (receiverEmail == null || receiverEmail.isBlank())
            return "Email du destinataire manquant";

        BigDecimal customs = getCellDecimal(row, cols[COL_CUSTOMS_VALUE]);
        if (customs == null)
            return "Valeur douanière manquante";
        if (customs.compareTo(BigDecimal.ZERO) <= 0)
            return "Valeur douanière doit être > 0 (valeur actuelle : " + customs + ")";

        if (!receiverEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return "Format email invalide : " + receiverEmail;

        if (receiverEmail.length() > MAX_EMAIL_LENGTH)
            return "Email trop long (" + receiverEmail.length() + " chars, max " + MAX_EMAIL_LENGTH + ")";

        if (hawb.length() > MAX_HAWB_LENGTH)
            return "HAWB trop long (" + hawb.length() + " chars, max " + MAX_HAWB_LENGTH + ")";

        if (mawb.length() > MAX_MAWB_LENGTH)
            return "MAWB trop long (" + mawb.length() + " chars, max " + MAX_MAWB_LENGTH + ")";

        if (receiverName.length() > MAX_NAME_LENGTH)
            return "Nom destinataire trop long (" + receiverName.length() + " chars, max " + MAX_NAME_LENGTH + ")";

        if (customs.compareTo(MAX_CUSTOMS_VALUE) > 0)
            return "Valeur douanière anormalement élevée : " + customs + " (max " + MAX_CUSTOMS_VALUE + ")";

        BigDecimal weight = getCellDecimal(row, cols[COL_WEIGHT]);
        if (weight == null)
            return "Poids du colis manquant";
        if (weight.compareTo(BigDecimal.ZERO) <= 0)
            return "Poids doit être > 0 (valeur actuelle : " + weight + ")";
        if (weight.compareTo(MAX_WEIGHT) > 0)
            return "Poids anormalement élevé : " + weight + " kg (max " + MAX_WEIGHT + ")";

        Integer nbItems = getCellInteger(row, cols[COL_NB_ITEMS]);
        if (nbItems != null && nbItems <= 0)
            return "Nombre d'articles doit être > 0 (valeur actuelle : " + nbItems + ")";
        if (nbItems != null && nbItems > 10000)
            return "Nombre d'articles anormalement élevé : " + nbItems;

        String description = getCellString(row, cols[COL_GOODS_DESC]);
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH)
            return "Description trop longue (" + description.length() + " chars, max " + MAX_DESCRIPTION_LENGTH + ")";

        return null;
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
