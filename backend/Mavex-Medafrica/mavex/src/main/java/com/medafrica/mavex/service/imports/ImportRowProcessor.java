package com.medafrica.mavex.service.imports;

import com.medafrica.mavex.dto.imports.ImportConfirmRequest;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.medafrica.mavex.service.imports.ImportColumns.*;

/**
 * Traite une ligne d'import dans SA PROPRE transaction (REQUIRES_NEW).
 *
 * <p>Pourquoi un bean séparé : Spring n'applique {@code @Transactional} que
 * sur les appels passant par le proxy. Si la boucle de {@code ExcelImportServiceImpl}
 * appelait {@code this.processRow(...)}, la propagation REQUIRES_NEW serait ignorée
 * et une erreur DB sur une ligne annulerait TOUT l'import.</p>
 *
 * <p>Ici, chaque ligne est isolée : une violation de contrainte (HAWB dupliqué,
 * longueur dépassée…) ne fait rollback que de cette ligne. Les lignes déjà
 * importées restent commitées.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportRowProcessor {

    private final ShipmentRepository shipmentRepository;
    private final ShipperRepository  shipperRepository;
    private final ClientRepository   clientRepository;
    private final OrderRepository    orderRepository;
    private final CountryRepository  countryRepository;
    private final AirlineRepository  airlineRepository;

    // ===============================================================
    // FLUX DIRECT — importManifest (lecture depuis Row Excel)
    // ===============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowOutcome processExcelRow(Row row, String mawb, String hawb, int[] cols, User currentUser) {

        // Doublons (re-checkés dans la transaction de la ligne)
        if (orderRepository.existsByHawbAndShipmentMawb(hawb, mawb)) {
            return RowOutcome.skipped("Doublon détecté : HAWB=" + hawb + " déjà importé sous le MAWB=" + mawb);
        }
        if (orderRepository.existsByHawb(hawb)) {
            return RowOutcome.skipped("HAWB=" + hawb + " déjà existant dans la base sous un autre MAWB");
        }

        List<String> warnings = new ArrayList<>();
        Shipment shipment = findOrCreateShipment(mawb, currentUser);
        Shipper  shipper  = findOrCreateShipperFromRow(row, cols, warnings);
        if (shipment.getShipper() == null) {
            shipment.setShipper(shipper);
            shipmentRepository.save(shipment);
        }
        Client client = findOrCreateClientFromRow(row, cols, warnings);
        createOrderFromRow(row, hawb, shipment, client, cols, warnings);

        String warningText = warnings.isEmpty() ? null : String.join(" | ", warnings);
        return RowOutcome.imported(warningText, mawb);
    }

    // ===============================================================
    // FLUX PREVIEW/CONFIRM — confirmImport (lecture depuis RowData)
    // ===============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowOutcome processConfirmRow(ImportConfirmRequest.RowData d, User currentUser) {

        String hawb = d.getHawb();
        String mawb = d.getMawb();

        if (orderRepository.existsByHawbAndShipmentMawb(hawb, mawb)) {
            return RowOutcome.skipped("Doublon MAWB+HAWB");
        }
        if (orderRepository.existsByHawb(hawb)) {
            return RowOutcome.skipped("HAWB existant sous autre MAWB");
        }

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
        return RowOutcome.imported(warningText, mawb);
    }

    // ===============================================================
    // SHIPMENT
    // ===============================================================

    private Shipment findOrCreateShipment(String mawb, User createdBy) {
        return shipmentRepository.findFirstByMawbOrderByCreatedAtDesc(mawb).orElseGet(() -> {
            Shipment.ShipmentBuilder builder = Shipment.builder()
                    .mawb(mawb)
                    .createdBy(createdBy);

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

    // ===============================================================
    // SHIPPER
    // ===============================================================

    private Shipper findOrCreateShipperFromRow(Row row, int[] cols, List<String> warnings) {
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
            assertShipperActive(existing);
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
            return shipperRepository.save(Shipper.builder()
                    .companyName(name)
                    .contactName(getCellString(row, cols[COL_SENDER_CONTACT]))
                    .email(email)
                    .phone(getCellString(row, cols[COL_SENDER_PHONE]))
                    .address(getCellString(row, cols[COL_SENDER_ADDRESS]))
                    .city(getCellString(row, cols[COL_SENDER_CITY]))
                    .locationName(getCellString(row, cols[COL_SENDER_CITY]))
                    .state(finalState)
                    .zipCode(getCellString(row, cols[COL_SENDER_POSTCODE]))
                    .country(senderCountry)
                    .build());
        });
    }

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
            assertShipperActive(existing);
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
            return shipperRepository.save(Shipper.builder()
                    .companyName(finalName).contactName(d.getSenderContact())
                    .email(email).phone(d.getSenderPhone())
                    .address(d.getSenderAddress()).city(d.getSenderCity())
                    .locationName(d.getSenderCity()).state(finalState)
                    .zipCode(d.getSenderPostcode()).country(country).build());
        });
    }

    private void assertShipperActive(Shipper existing) {
        if (!existing.isActive()) {
            throw new IllegalArgumentException("Le shipper '" + existing.getCompanyName()
                    + "' est désactivé. Veuillez le réactiver avant d'importer.");
        }
    }

    // ===============================================================
    // CLIENT
    // ===============================================================

    private Client findOrCreateClientFromRow(Row row, int[] cols, List<String> warnings) {
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
            assertClientUsable(existing, fullName, email);
            existing.setPhone(getCellString(row, cols[COL_RECEIVER_PHONE]));
            existing.setAddress(getCellString(row, cols[COL_RECEIVER_ADDRESS]));
            existing.setCity(getCellString(row, cols[COL_RECEIVER_CITY]));
            existing.setState(finalState);
            existing.setZipCode(getCellString(row, cols[COL_RECEIVER_POSTCODE]));
            existing.setContactName(getCellString(row, cols[COL_RECEIVER_CONTACT]));
            if (country != null) existing.setCountry(country);
            return clientRepository.save(existing);
        }).orElseGet(() -> clientRepository.save(Client.builder()
                .fullName(fullName).email(email)
                .phone(getCellString(row, cols[COL_RECEIVER_PHONE]))
                .address(getCellString(row, cols[COL_RECEIVER_ADDRESS]))
                .city(getCellString(row, cols[COL_RECEIVER_CITY]))
                .state(finalState)
                .zipCode(getCellString(row, cols[COL_RECEIVER_POSTCODE]))
                .contactName(getCellString(row, cols[COL_RECEIVER_CONTACT]))
                .country(country).build()));
    }

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
            assertClientUsable(existing, fullName, email);
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

    /** Refuse un client désactivé ou dont l'email appartient à quelqu'un d'autre. */
    private void assertClientUsable(Client existing, String fullName, String email) {
        if (!existing.isActive()) {
            throw new IllegalArgumentException("Le client '" + existing.getEmail()
                    + "' est désactivé. Veuillez le réactiver avant d'importer.");
        }
        if (existing.getFullName() != null && fullName != null
                && !existing.getFullName().equalsIgnoreCase(fullName)) {
            throw new IllegalArgumentException(
                    "L'email '" + email + "' appartient déjà au client '" + existing.getFullName()
                    + "'. Impossible de l'associer à '" + fullName + "'.");
        }
    }

    // ===============================================================
    // ORDER
    // ===============================================================

    private void createOrderFromRow(Row row, String hawb, Shipment shipment, Client client, int[] cols, List<String> warnings) {
        String currency = normalizeCurrency(getCellString(row, cols[COL_CURRENCY]), warnings);
        orderRepository.save(Order.builder()
                .hawb(hawb)
                .alternateReference(getCellString(row, cols[COL_ALTERNATE_REF]))
                .goodsDescription(getCellString(row, cols[COL_GOODS_DESC]))
                .numberOfItems(getCellInteger(row, cols[COL_NB_ITEMS]))
                .shipmentWeight(getCellDecimal(row, cols[COL_WEIGHT]))
                .customsValue(getCellDecimal(row, cols[COL_CUSTOMS_VALUE]))
                .customsCurrency(currency)
                .dutyRate(shipment.getDutyRate())
                .shipment(shipment).client(client)
                .status(OrderStatus.CREATED).build());
    }

    private void createOrderFromRequest(ImportConfirmRequest.RowData d, Shipment shipment, Client client, List<String> warnings) {
        String currency = normalizeCurrency(d.getCustomsCurrency(), warnings);
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

    private String normalizeCurrency(String currency, List<String> warnings) {
        if (currency == null || currency.isBlank()) {
            warnings.add("Currency manquante → forcée à 'USD'");
            return "USD";
        }
        if (!currency.matches("[A-Z]{3}")) {
            warnings.add("Currency invalide '" + currency + "' → forcée à 'USD'");
            return "USD";
        }
        return currency;
    }

    // ===============================================================
    // PAYS
    // ===============================================================

    private Country resolveCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return null;
        Country c = countryRepository.findById(countryCode.trim()).orElse(null);
        if (c != null) return c;
        c = countryRepository.findById(countryCode.trim().toUpperCase()).orElse(null);
        if (c != null) return c;
        c = countryRepository.findByNameIgnoreCase(countryCode.trim()).orElse(null);
        if (c != null) return c;
        log.warn("Pays '{}' introuvable — entité importée sans pays lié", countryCode);
        return null;
    }

    // ===============================================================
    // HELPERS — lecture cellules Excel
    // ===============================================================

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
}
