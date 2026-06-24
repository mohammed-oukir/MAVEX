package com.medafrica.mavex.service.finance;

import com.medafrica.mavex.dto.finance.ExchangeRateImportResult;
import com.medafrica.mavex.dto.finance.ExchangeRateRequest;
import com.medafrica.mavex.dto.finance.ExchangeRateResponse;
import com.medafrica.mavex.model.finance.ExchangeRate;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.ExchangeRateRepository;
import com.medafrica.mavex.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final List<String> REQUIRED_HEADERS = List.of(
        "fromcurrency", "tocurrency", "rate", "effectivedate", "source", "isactive"
    );

    private final ExchangeRateRepository        exchangeRateRepository;
    private final ExchangeRatePersistenceService persistenceService;
    private final NotificationService            notificationService;

    // ---------------------------------------------------------------
    // IMPORT — Excel (.xlsx) ou CSV (.csv)
    // ---------------------------------------------------------------

    public ExchangeRateImportResult importFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Le fichier est vide ou absent.");

        String filename = file.getOriginalFilename();
        if (filename == null)
            throw new IllegalArgumentException("Nom de fichier introuvable.");

        String lower = filename.toLowerCase();
        ExchangeRateImportResult result;
        if (lower.endsWith(".xlsx"))     result = importExcel(file, filename);
        else if (lower.endsWith(".csv")) result = importCsv(file, filename);
        else throw new IllegalArgumentException(
            "Format non supporté : utilisez .xlsx ou .csv (fichier reçu : " + filename + ")."
        );

        if (result.getImported() > 0) {
            User currentUser = getCurrentUser();
            String importerName = currentUser != null ? currentUser.getFullName() : "Système";
            String msg = "Le comptable " + importerName + " a importé "
                + result.getImported() + " taux de change le "
                + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            notificationService.createForAllAdmins("Nouveau taux importé", msg, "/exchange-rates");
        }

        return result;
    }

    // ---------------------------------------------------------------
    // LECTURE — Excel
    // ---------------------------------------------------------------

    private ExchangeRateImportResult importExcel(MultipartFile file, String filename) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() < 1)
                throw new IllegalArgumentException("Le fichier Excel est vide ou ne contient que l'en-tête.");

            Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                throw new IllegalArgumentException("La ligne d'en-tête est absente du fichier Excel.");

            Map<String, Integer> headerMap = buildExcelHeaderMap(headerRow);
            validateRequiredHeaders(headerMap, filename);

            User currentUser = getCurrentUser();
            int imported = 0, skipped = 0, errors = 0;
            List<String> errorDetails = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                int rowNumber = i + 1;
                try {
                    String fromCurrency = requireString(getCellString(row, headerMap, "fromcurrency"), "fromCurrency", rowNumber);
                    String toCurrency   = requireString(getCellString(row, headerMap, "tocurrency"),   "toCurrency",   rowNumber);
                    BigDecimal rate     = requireDecimal(getCellDecimal(row, headerMap, "rate"),        "rate",         rowNumber);
                    LocalDate effDate   = requireDate(getCellString(row, headerMap, "effectivedate"),   "effectiveDate", rowNumber);
                    String source       = getCellString(row, headerMap, "source");
                    Boolean isActive    = parseBooleanCell(getCellString(row, headerMap, "isactive"),   rowNumber);

                    if (Boolean.FALSE.equals(isActive)) {
                        skipped++;
                        log.debug("Ligne {} ignorée (isActive=false) — {}/{}", rowNumber, fromCurrency, toCurrency);
                        continue;
                    }

                    persistenceService.deactivateAndSave(
                        fromCurrency.toUpperCase(), toCurrency.toUpperCase(),
                        rate, effDate, source, currentUser);
                    imported++;
                    log.info("Ligne {} importée — {}/{} = {}", rowNumber, fromCurrency, toCurrency, rate);

                } catch (IllegalArgumentException e) {
                    errors++;
                    errorDetails.add("Ligne " + rowNumber + " : " + e.getMessage());
                    log.warn("Ligne {} rejetée : {}", rowNumber, e.getMessage());
                }
            }

            log.info("Import Excel '{}' terminé — importé={}, ignoré={}, erreurs={}", filename, imported, skipped, errors);
            return ExchangeRateImportResult.builder()
                .imported(imported).skipped(skipped).errors(errors)
                .errorDetails(errorDetails).build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lecture Excel '{}' : {}", filename, e.getMessage(), e);
            throw new IllegalArgumentException("Fichier Excel illisible ou corrompu : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // LECTURE — CSV
    // ---------------------------------------------------------------

    private ExchangeRateImportResult importCsv(MultipartFile file, String filename) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String headerLine = reader.readLine();
            if (headerLine == null)
                throw new IllegalArgumentException("Le fichier CSV est vide.");

            String[] headers = headerLine.split("[;,]");
            Map<String, Integer> headerMap = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim().toLowerCase().replace(" ", ""), i);
            }
            validateRequiredHeaders(headerMap, filename);

            User currentUser = getCurrentUser();
            int imported = 0, skipped = 0, errors = 0;
            List<String> errorDetails = new ArrayList<>();
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;

                String[] cells = line.split("[;,]", -1);
                try {
                    String fromCurrency = requireString(getCell(cells, headerMap, "fromcurrency"), "fromCurrency", lineNumber);
                    String toCurrency   = requireString(getCell(cells, headerMap, "tocurrency"),   "toCurrency",   lineNumber);
                    BigDecimal rate     = requireDecimal(parseDecimal(getCell(cells, headerMap, "rate")), "rate", lineNumber);
                    LocalDate effDate   = requireDate(getCell(cells, headerMap, "effectivedate"),   "effectiveDate", lineNumber);
                    String source       = getCell(cells, headerMap, "source");
                    Boolean isActive    = parseBooleanCell(getCell(cells, headerMap, "isactive"),   lineNumber);

                    if (Boolean.FALSE.equals(isActive)) {
                        skipped++;
                        continue;
                    }

                    persistenceService.deactivateAndSave(
                        fromCurrency.toUpperCase(), toCurrency.toUpperCase(),
                        rate, effDate, source, currentUser);
                    imported++;

                } catch (IllegalArgumentException e) {
                    errors++;
                    errorDetails.add("Ligne " + lineNumber + " : " + e.getMessage());
                    log.warn("CSV ligne {} rejetée : {}", lineNumber, e.getMessage());
                }
            }

            log.info("Import CSV '{}' terminé — importé={}, ignoré={}, erreurs={}", filename, imported, skipped, errors);
            return ExchangeRateImportResult.builder()
                .imported(imported).skipped(skipped).errors(errors)
                .errorDetails(errorDetails).build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lecture CSV '{}' : {}", filename, e.getMessage(), e);
            throw new IllegalArgumentException("Fichier CSV illisible : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // LECTURE — liste et lookups
    // ---------------------------------------------------------------

    public List<ExchangeRateResponse> findAll(String fromCurrency, String toCurrency, Boolean isActive, LocalDate effectiveDate) {
        List<ExchangeRate> all = (Boolean.TRUE.equals(isActive) && fromCurrency == null && toCurrency == null)
            ? exchangeRateRepository.findAllByIsActiveTrue()
            : exchangeRateRepository.findAll();

        return all.stream()
            .filter(e -> fromCurrency  == null || e.getFromCurrency().equalsIgnoreCase(fromCurrency))
            .filter(e -> toCurrency    == null || e.getToCurrency().equalsIgnoreCase(toCurrency))
            .filter(e -> isActive      == null || e.getIsActive().equals(isActive))
            .filter(e -> effectiveDate == null || e.getEffectiveDate().equals(effectiveDate))
            .map(this::toResponse)
            .toList();
    }

    public ExchangeRateResponse findActive(String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency))
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Les deux devises ne peuvent pas être identiques");
        return exchangeRateRepository
            .findByIsActiveTrueAndFromCurrencyAndToCurrency(
                fromCurrency.toUpperCase(), toCurrency.toUpperCase())
            .map(this::toResponse)
            .orElseThrow(() -> new IllegalArgumentException(
                "Aucun taux actif trouvé pour " + fromCurrency + " → " + toCurrency));
    }

    public ExchangeRateResponse findByPairAndDate(String fromCurrency, String toCurrency, LocalDate date) {
        return exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndEffectiveDate(
                fromCurrency.toUpperCase(), toCurrency.toUpperCase(), date)
            .map(this::toResponse)
            .orElseThrow(() -> new IllegalArgumentException(
                "Aucun taux trouvé pour " + fromCurrency + " → " + toCurrency + " à la date " + date));
    }

    public void delete(Long id) {
        if (!exchangeRateRepository.existsById(id))
            throw new IllegalArgumentException("Taux de change introuvable : id=" + id);
        persistenceService.delete(id);
    }

    public List<String> getCurrencies() {
        return exchangeRateRepository.findDistinctCurrencies();
    }

    // ---------------------------------------------------------------
    // CRUD MANUEL — créer et modifier
    // ---------------------------------------------------------------

    public ExchangeRateResponse createManual(ExchangeRateRequest req) {
        validateRequest(req);
        User currentUser = getCurrentUser();
        persistenceService.deactivateAndSave(
            req.getFromCurrency().toUpperCase(),
            req.getToCurrency().toUpperCase(),
            req.getRate(),
            req.getEffectiveDate(),
            req.getSource(),
            currentUser
        );
        ExchangeRate saved = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndEffectiveDate(
                req.getFromCurrency().toUpperCase(),
                req.getToCurrency().toUpperCase(),
                req.getEffectiveDate())
            .orElseThrow(() -> new IllegalStateException("Erreur inattendue lors de la création du taux."));

        String importerName = currentUser != null ? currentUser.getFullName() : "Système";
        String msg = "Le comptable " + importerName + " a ajouté "
            + saved.getFromCurrency() + "→" + saved.getToCurrency()
            + " : " + saved.getRate()
            + " le " + saved.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        notificationService.createForAllAdmins("Taux ajouté manuellement", msg, "/exchange-rates");

        return toResponse(saved);
    }

    public ExchangeRateResponse update(Long id, ExchangeRateRequest req) {
        validateRequest(req);
        ExchangeRate entity = exchangeRateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Taux de change introuvable : id=" + id));

        BigDecimal oldRate = entity.getRate();
        User currentUser   = getCurrentUser();

        entity.setRate(req.getRate());
        entity.setFromCurrency(req.getFromCurrency().toUpperCase());
        entity.setToCurrency(req.getToCurrency().toUpperCase());
        entity.setEffectiveDate(req.getEffectiveDate());
        entity.setSource(req.getSource());
        entity.setImportedBy(currentUser);
        entity.setImportedAt(java.time.LocalDateTime.now());
        exchangeRateRepository.save(entity);

        String importerName = currentUser != null ? currentUser.getFullName() : "Système";
        String msg = "Le comptable " + importerName + " a modifié "
            + entity.getFromCurrency() + "→" + entity.getToCurrency()
            + " : " + oldRate + " → " + entity.getRate()
            + " le " + entity.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        notificationService.createForAllAdmins("Taux modifié", msg, "/exchange-rates");

        return toResponse(entity);
    }

    // ---------------------------------------------------------------
    // VALIDATION — request manuelle
    // ---------------------------------------------------------------

    private void validateRequest(ExchangeRateRequest req) {
        if (req.getFromCurrency() == null || req.getFromCurrency().isBlank())
            throw new IllegalArgumentException("fromCurrency est obligatoire.");
        if (req.getFromCurrency().trim().length() > 3)
            throw new IllegalArgumentException("fromCurrency doit être un code ISO 3 lettres (ex: USD).");
        if (req.getToCurrency() == null || req.getToCurrency().isBlank())
            throw new IllegalArgumentException("toCurrency est obligatoire.");
        if (req.getToCurrency().trim().length() > 3)
            throw new IllegalArgumentException("toCurrency doit être un code ISO 3 lettres (ex: MAD).");
        if (req.getRate() == null)
            throw new IllegalArgumentException("rate est obligatoire.");
        if (req.getRate().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("rate doit être > 0.");
        if (req.getEffectiveDate() == null)
            throw new IllegalArgumentException("effectiveDate est obligatoire.");
    }

    // ---------------------------------------------------------------
    // MAPPING entité → DTO
    // ---------------------------------------------------------------

    private ExchangeRateResponse toResponse(ExchangeRate e) {
        return ExchangeRateResponse.builder()
            .id(e.getId())
            .fromCurrency(e.getFromCurrency())
            .toCurrency(e.getToCurrency())
            .rate(e.getRate())
            .effectiveDate(e.getEffectiveDate())
            .source(e.getSource())
            .isActive(e.getIsActive())
            .importedByName(e.getImportedBy() != null ? e.getImportedBy().getFullName() : null)
            .importedAt(e.getImportedAt())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .build();
    }

    // ---------------------------------------------------------------
    // HELPERS — validation headers
    // ---------------------------------------------------------------

    private void validateRequiredHeaders(Map<String, Integer> headerMap, String filename) {
        List<String> missing = REQUIRED_HEADERS.stream()
            .filter(h -> !headerMap.containsKey(h))
            .map(h -> "\"" + h + "\"")
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "Colonnes obligatoires introuvables dans '" + filename + "' : "
                + String.join(", ", missing)
                + ". En-têtes attendus (insensible à la casse) : fromCurrency, toCurrency, rate, effectiveDate, source, isActive."
            );
        }
    }

    private String requireString(String val, String field, int row) {
        if (val == null || val.isBlank())
            throw new IllegalArgumentException("Champ '" + field + "' obligatoire manquant.");
        if (field.toLowerCase().contains("currency") && val.trim().length() > 3)
            throw new IllegalArgumentException(
                "Champ '" + field + "' doit être un code ISO 3 lettres (ex: USD, MAD). Reçu : '" + val + "'.");
        return val.trim();
    }

    private BigDecimal requireDecimal(BigDecimal val, String field, int row) {
        if (val == null)
            throw new IllegalArgumentException("Champ '" + field + "' obligatoire manquant ou non numérique.");
        if (val.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Champ '" + field + "' doit être > 0. Reçu : " + val + ".");
        return val;
    }

    private LocalDate requireDate(String val, String field, int row) {
        if (val == null || val.isBlank())
            throw new IllegalArgumentException("Champ '" + field + "' obligatoire manquant.");
        for (DateTimeFormatter fmt : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"))) {
            try { return LocalDate.parse(val.trim(), fmt); } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException(
            "Champ '" + field + "' : format de date non reconnu '" + val + "'. Utilisez yyyy-MM-dd ou dd/MM/yyyy.");
    }

    private Boolean parseBooleanCell(String val, int row) {
        if (val == null || val.isBlank()) return true;
        return switch (val.trim().toLowerCase()) {
            case "true",  "1", "oui", "yes", "actif"   -> true;
            case "false", "0", "non", "no",  "inactif"  -> false;
            default -> throw new IllegalArgumentException(
                "Champ 'isActive' : valeur non reconnue '" + val + "'. Utilisez true/false ou 1/0.");
        };
    }

    // ---------------------------------------------------------------
    // HELPERS — lecture cellules Excel
    // ---------------------------------------------------------------

    private Map<String, Integer> buildExcelHeaderMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int c = 0; c <= headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String text = switch (cell.getCellType()) {
                case STRING  -> cell.getStringCellValue().trim();
                case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
                default -> null;
            };
            if (text != null && !text.isEmpty())
                map.put(text.toLowerCase().replace(" ", ""), c);
        }
        return map;
    }

    private String getCellString(Row row, Map<String, Integer> headerMap, String key) {
        Integer col = headerMap.get(key);
        if (col == null) return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> { String v = cell.getStringCellValue().trim(); yield v.isEmpty() ? null : v; }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private BigDecimal getCellDecimal(Row row, Map<String, Integer> headerMap, String key) {
        return parseDecimal(getCellString(row, headerMap, key));
    }

    // ---------------------------------------------------------------
    // HELPERS — CSV
    // ---------------------------------------------------------------

    private String getCell(String[] cells, Map<String, Integer> headerMap, String key) {
        Integer idx = headerMap.get(key);
        if (idx == null || idx >= cells.length) return null;
        String v = cells[idx].trim().replaceAll("^\"|\"$", "");
        return v.isEmpty() ? null : v;
    }

    // ---------------------------------------------------------------
    // HELPERS — parsing
    // ---------------------------------------------------------------

    private BigDecimal parseDecimal(String val) {
        if (val == null || val.isBlank()) return null;
        try { return new BigDecimal(val.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u : null;
    }
}
