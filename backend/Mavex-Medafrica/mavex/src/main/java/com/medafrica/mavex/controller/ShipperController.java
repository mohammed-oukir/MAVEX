package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.ApiResponse;
import com.medafrica.mavex.dto.shipper.ShipperRequestDTO;
import com.medafrica.mavex.dto.shipper.ShipperResponseDTO;
import com.medafrica.mavex.service.interfaces.ShipperService;
import org.springframework.data.domain.Sort;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

   /** GET /api/shippers */
@GetMapping
public ResponseEntity<Page<ShipperResponseDTO>> list(
        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(shipperService.list(pageable));
}
    /** GET /api/shippers/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ShipperResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipperService.getById(id));
    }

    /** POST /api/shippers */
    @PostMapping
    public ResponseEntity<ApiResponse<ShipperResponseDTO>> create(@Valid @RequestBody ShipperRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.<ShipperResponseDTO>builder()
                .message("Shipper créé avec succès")
                .data(shipperService.create(dto))
                .build()
        );
    }

    /** PUT /api/shippers/{id} - mise à jour complète */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipperResponseDTO>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ShipperRequestDTO dto) {
        return ResponseEntity.ok(
            ApiResponse.<ShipperResponseDTO>builder()
                .message("Shipper modifié avec succès")
                .data(shipperService.update(id, dto))
                .build()
        );
    }

    /** PATCH /api/shippers/{id} - mise à jour partielle */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipperResponseDTO>> patch(@PathVariable Long id,
                                                                  @RequestBody ShipperRequestDTO dto) {
        return ResponseEntity.ok(
            ApiResponse.<ShipperResponseDTO>builder()
                .message("Shipper mis à jour partiellement avec succès")
                .data(shipperService.patch(id, dto))
                .build()
        );
    }

    /** PATCH /api/shippers/{id}/deactivate - soft delete */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        shipperService.deactivate(id);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .message("Shipper désactivé avec succès")
                .data(null)
                .build()
        );
    }


/** PATCH /api/shippers/{id}/activate - réactivation */
@PatchMapping("/{id}/activate")
public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
    shipperService.activate(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .message("Shipper activé avec succès")
            .data(null)
            .build()
    );
}

    /** DELETE /api/shippers/{id} - hard delete ADMIN */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shipperService.delete(id);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .message("Shipper supprimé définitivement")
                .data(null)
                .build()
        );
    }
}