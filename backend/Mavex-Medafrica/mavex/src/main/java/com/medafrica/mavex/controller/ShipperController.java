package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.ApiResponse;
import com.medafrica.mavex.dto.shipper.ShipperRequestDTO;
import com.medafrica.mavex.dto.shipper.ShipperResponseDTO;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.interfaces.ShipperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shippers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class ShipperController {

    private final ShipperService shipperService;

    @GetMapping
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.VIEW)
    public ResponseEntity<Page<ShipperResponseDTO>> list(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(shipperService.list(pageable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.VIEW)
    public ResponseEntity<ShipperResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipperService.getById(id));
    }

    @PostMapping
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.CREATE)
    public ResponseEntity<ApiResponse<ShipperResponseDTO>> create(@Valid @RequestBody ShipperRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.<ShipperResponseDTO>builder()
                .message("Shipper créé avec succès")
                .data(shipperService.create(dto))
                .build()
        );
    }

    @PutMapping("/{id}")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiResponse<ShipperResponseDTO>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody ShipperRequestDTO dto) {
        return ResponseEntity.ok(
            ApiResponse.<ShipperResponseDTO>builder()
                .message("Shipper modifié avec succès")
                .data(shipperService.update(id, dto))
                .build()
        );
    }

    @PatchMapping("/{id}")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiResponse<ShipperResponseDTO>> patch(@PathVariable Long id,
                                                                 @RequestBody ShipperRequestDTO dto) {
        return ResponseEntity.ok(
            ApiResponse.<ShipperResponseDTO>builder()
                .message("Shipper mis à jour partiellement avec succès")
                .data(shipperService.patch(id, dto))
                .build()
        );
    }

    @PatchMapping("/{id}/activate")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        shipperService.activate(id);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .message("Shipper activé avec succès")
                .data(null)
                .build()
        );
    }

    @PatchMapping("/{id}/deactivate")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.EDIT)
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        shipperService.deactivate(id);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .message("Shipper désactivé avec succès")
                .data(null)
                .build()
        );
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.DELETE)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shipperService.delete(id);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .message("Shipper supprimé définitivement")
                .data(null)
                .build()
        );
    }

    @PostMapping("/bulk-delete")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.DELETE)
    public ResponseEntity<Map<String, Integer>> bulkDelete(@RequestBody Map<String, List<Long>> body) {
        int count = shipperService.bulkDelete(body.get("ids"));
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @PostMapping("/bulk-activate")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.EDIT)
    public ResponseEntity<Map<String, Integer>> bulkActivate(@RequestBody Map<String, List<Long>> body) {
        int count = shipperService.bulkActivate(body.get("ids"));
        return ResponseEntity.ok(Map.of("activated", count));
    }

    @PostMapping("/bulk-deactivate")
    @RequiresPermission(module = PermissionModule.SHIPPERS, action = PermissionAction.EDIT)
    public ResponseEntity<Map<String, Integer>> bulkDeactivate(@RequestBody Map<String, List<Long>> body) {
        int count = shipperService.bulkDeactivate(body.get("ids"));
        return ResponseEntity.ok(Map.of("deactivated", count));
    }
}
