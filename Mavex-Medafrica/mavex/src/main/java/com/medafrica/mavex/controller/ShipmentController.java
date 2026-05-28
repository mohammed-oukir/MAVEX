package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.shipment.ShipmentRequestDTO;
import com.medafrica.mavex.dto.shipment.ShipmentResponseDTO;
import com.medafrica.mavex.dto.shipment.ShipmentStatusUpdateDTO;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.service.interfaces.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // POST /api/v1/shipments
    @PostMapping
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ShipmentResponseDTO> create(@Valid @RequestBody ShipmentRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.create(req));
    }

    // GET /api/v1/shipments/{id}
    @GetMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ShipmentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getById(id));
    }

    // GET /api/v1/shipments?page=0&size=20
    @GetMapping
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Page<ShipmentResponseDTO>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(shipmentService.list(pageable));
    }

    // GET /api/v1/shipments/status/IMPORTED?page=0&size=20
    @GetMapping("/status/{status}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Page<ShipmentResponseDTO>> listByStatus(
            @PathVariable ShipmentStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(shipmentService.listByStatus(status, pageable));
    }

    // PATCH /api/v1/shipments/{id}
    @PatchMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ShipmentResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ShipmentRequestDTO req) {   // pas @Valid : champs optionnels au update
        return ResponseEntity.ok(shipmentService.update(id, req));
    }

    // PATCH /api/v1/shipments/{id}/status
    @PatchMapping("/{id}/status")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ShipmentResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateDTO req) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, req));
    }

    // PUT /api/shipments/{id}
@PutMapping("/{id}")
//@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public ResponseEntity<ShipmentResponseDTO> replace(
        @PathVariable Long id,
        @Valid @RequestBody ShipmentRequestDTO req) {
    return ResponseEntity.ok(shipmentService.replace(id, req));
}

    // DELETE /api/v1/shipments/{id}  — ADMIN seulement
    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}