package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.shipment.DutyChangeHistoryResponse;
import com.medafrica.mavex.dto.shipment.DutyRateUpdateDTO;
import com.medafrica.mavex.dto.shipment.ExportSelectionRequest;
import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.logistics.Shipment;
import com.medafrica.mavex.service.export.TableExportService;
import com.medafrica.mavex.service.interfaces.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final TableExportService exportService;

    @PostMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','CREATE')")
    public ResponseEntity<ShipmentResponseDTO> create(@Valid @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.create(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','VIEW')")
    public ResponseEntity<ShipmentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getById(id));
    }

    // ─────────── GET /api/shipments/{id}/duty-history/shipment — historique SHIPMENT ───────────
    @GetMapping("/{id}/duty-history/shipment")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','VIEW')")
    public ResponseEntity<Page<DutyChangeHistoryResponse>> getShipmentDutyHistory(
            @PathVariable Long id,
            @RequestParam(required = false) String changedByName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(shipmentService.getShipmentDutyHistory(id, changedByName, from, to, pageable));
    }

    // ─────────── GET /api/shipments/{id}/duty-history/orders — historique ORDER (tous les orders du shipment) ───────────
    @GetMapping("/{id}/duty-history/orders")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','VIEW')")
    public ResponseEntity<Page<DutyChangeHistoryResponse>> getOrderDutyHistory(
            @PathVariable Long id,
            @RequestParam(required = false) String hawb,
            @RequestParam(required = false) String changedByName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(shipmentService.getOrderDutyHistoryForShipment(id, hawb, changedByName, from, to, pageable));
    }

    // ─────────── GET /api/shipments — liste paginée, avec filtres par colonne ───────────
    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','VIEW')")
    public ResponseEntity<Page<ShipmentResponseDTO>> list(
            @RequestParam(required = false) String mawb,
            @RequestParam(required = false) String shipper,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importTo,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer totalOrders,
            @RequestParam(required = false) Double dutyRateMin,
            @RequestParam(required = false) Double dutyRateMax,
            @RequestParam(required = false) ShipmentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        boolean hasFilter = mawb != null || shipper != null || importFrom != null || importTo != null
                || carrier != null || mode != null || totalOrders != null
                || dutyRateMin != null || dutyRateMax != null || status != null;

        if (!hasFilter) {
            return ResponseEntity.ok(shipmentService.list(pageable));
        }

        return ResponseEntity.ok(shipmentService.search(
                mawb, shipper, importFrom, importTo, carrier, mode,
                totalOrders, dutyRateMin, dutyRateMax, status, pageable));
    }

    // ─────────── GET /api/shipments/export/pdf — export PDF des shipments filtrés ───────────
    @GetMapping("/export/pdf")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','EXPORT')")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String mawb,
            @RequestParam(required = false) String shipper,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importTo,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer totalOrders,
            @RequestParam(required = false) Double dutyRateMin,
            @RequestParam(required = false) Double dutyRateMax,
            @RequestParam(required = false) ShipmentStatus status) {

        List<List<String>> rows = buildExportRows(
                mawb, shipper, importFrom, importTo, carrier, mode,
                totalOrders, dutyRateMin, dutyRateMax, status);

        byte[] bytes = exportService.generatePdf("Liste des shipments", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shipments.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ─────────── GET /api/shipments/export/excel — export Excel des shipments filtrés ───────────
    @GetMapping("/export/excel")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','EXPORT')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String mawb,
            @RequestParam(required = false) String shipper,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate importTo,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer totalOrders,
            @RequestParam(required = false) Double dutyRateMin,
            @RequestParam(required = false) Double dutyRateMax,
            @RequestParam(required = false) ShipmentStatus status) {

        List<List<String>> rows = buildExportRows(
                mawb, shipper, importFrom, importTo, carrier, mode,
                totalOrders, dutyRateMin, dutyRateMax, status);

        byte[] bytes = exportService.generateExcel("Shipments", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shipments.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ─────────── POST /api/shipments/export/excel/selection — export Excel d'une sélection d'ids ───────────
    @PostMapping("/export/excel/selection")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','EXPORT')")
    public ResponseEntity<byte[]> exportExcelSelection(@RequestBody ExportSelectionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("Veuillez sélectionner au moins un envoi.");
        }

        List<Shipment> shipments = shipmentService.findAllByIds(request.getIds());
        List<Long> ids = shipments.stream().map(Shipment::getId).toList();
        Map<Long, Long> orderCounts = shipmentService.countOrdersByShipmentIds(ids);
        List<List<String>> rows = rowsFromShipments(shipments, orderCounts);

        byte[] bytes = exportService.generateExcel("Shipments (sélection)", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shipments_selection.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static final List<String> EXPORT_HEADERS =
            List.of("MAWB", "Date arrivée", "Carrier", "Mode", "Orders", "Duty", "Statut");

    private static final DateTimeFormatter EXPORT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<List<String>> buildExportRows(
            String mawb, String shipper, LocalDate importFrom, LocalDate importTo,
            String carrier, String mode, Integer totalOrders,
            Double dutyRateMin, Double dutyRateMax, ShipmentStatus status) {

        List<Shipment> shipments = shipmentService.searchAll(
                mawb, shipper, importFrom, importTo, carrier, mode,
                totalOrders, dutyRateMin, dutyRateMax, status);

        List<Long> ids = shipments.stream().map(Shipment::getId).toList();
        Map<Long, Long> orderCounts = shipmentService.countOrdersByShipmentIds(ids);

        return rowsFromShipments(shipments, orderCounts);
    }

    private List<List<String>> rowsFromShipments(List<Shipment> shipments, Map<Long, Long> orderCounts) {
        List<List<String>> rows = new ArrayList<>();
        for (Shipment s : shipments) {
            String importDate = s.getImportDate() != null ? s.getImportDate().format(EXPORT_DATE_FMT) : "";
            String carrierVal = s.getImportingCarrier() != null ? s.getImportingCarrier() : "";
            String modeVal    = s.getModeOfTransport() != null ? s.getModeOfTransport() : "";
            long   ordersCount = orderCounts.getOrDefault(s.getId(), 0L);
            String dutyVal     = s.getDutyRate() != null
                    ? s.getDutyRate().multiply(BigDecimal.valueOf(100)) + "%"
                    : "";
            String statusName  = s.getStatus() != null ? s.getStatus().name() : "";

            rows.add(List.of(
                    s.getMawb() != null ? s.getMawb() : "",
                    importDate,
                    carrierVal,
                    modeVal,
                    String.valueOf(ordersCount),
                    dutyVal,
                    statusName
            ));
        }
        return rows;
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','VIEW')")
    public ResponseEntity<Page<ShipmentResponseDTO>> listByStatus(
            @PathVariable ShipmentStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(shipmentService.listByStatus(status, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','UPDATE')")
    public ResponseEntity<ShipmentResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.ok(shipmentService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','CHANGE_STATUS')")
    public ResponseEntity<ShipmentResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateDTO req) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','UPDATE')")
    public ResponseEntity<ShipmentResponseDTO> replace(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.ok(shipmentService.replace(id, req));
    }

    @PatchMapping("/{id}/duty-rate")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','UPDATE_DUTY_RATE')")
    public ResponseEntity<ShipmentResponseDTO> updateDutyRate(
            @PathVariable Long id,
            @Valid @RequestBody DutyRateUpdateDTO req) {
        return ResponseEntity.ok(shipmentService.updateDutyRate(id, req.getDutyRate()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('SHIPMENTS','DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
