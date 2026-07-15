package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.shipment.DutyRateUpdateDTO;
import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.interfaces.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.CREATE)
    public ResponseEntity<ShipmentResponseDTO> create(@Valid @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.create(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.VIEW)
    public ResponseEntity<ShipmentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getById(id));
    }

    // ─────────── GET /api/shipments — liste paginée, avec filtres par colonne ───────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.VIEW)
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

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.VIEW)
    public ResponseEntity<Page<ShipmentResponseDTO>> listByStatus(
            @PathVariable ShipmentStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(shipmentService.listByStatus(status, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.EDIT)
    public ResponseEntity<ShipmentResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.ok(shipmentService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.EDIT)
    public ResponseEntity<ShipmentResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateDTO req) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.EDIT)
    public ResponseEntity<ShipmentResponseDTO> replace(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.ok(shipmentService.replace(id, req));
    }

    @PatchMapping("/{id}/duty-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.EDIT)
    public ResponseEntity<ShipmentResponseDTO> updateDutyRate(
            @PathVariable Long id,
            @Valid @RequestBody DutyRateUpdateDTO req) {
        return ResponseEntity.ok(shipmentService.updateDutyRate(id, req.getDutyRate()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.SHIPMENTS, action = PermissionAction.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
