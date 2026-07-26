package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.client.BulkActionRequest;
import com.medafrica.mavex.dto.client.ClientPatchRequest;
import com.medafrica.mavex.dto.client.ClientRequestDTO;
import com.medafrica.mavex.dto.client.ClientResponseDTO;
import com.medafrica.mavex.dto.client.ClientSearchCriteria;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.interfaces.ClientService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.VIEW)
    public ResponseEntity<List<ClientResponseDTO>> getAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/active")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.VIEW)
    public ResponseEntity<List<ClientResponseDTO>> getAllActive() {
        return ResponseEntity.ok(clientService.findAllActive());
    }

    @GetMapping("/search")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.VIEW)
    public ResponseEntity<Page<ClientResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ClientSearchCriteria criteria = ClientSearchCriteria.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .city(city)
                .state(state)
                .country(country)
                .status(status)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
        return ResponseEntity.ok(clientService.search(criteria, pageable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.VIEW)
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.CREATE)
    public ResponseEntity<ClientResponseDTO> create(@Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }

    @PutMapping("/{id}")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.EDIT)
    public ResponseEntity<ClientResponseDTO> update(@PathVariable Long id,
                                                    @Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @PatchMapping("/{id}")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.EDIT)
    public ResponseEntity<ClientResponseDTO> patch(@PathVariable Long id,
                                                   @Valid @RequestBody ClientPatchRequest dto) {
        return ResponseEntity.ok(clientService.patch(id, dto));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.DELETE)
    public ResponseEntity<Map<String, Integer>> bulkDelete(@Valid @RequestBody BulkActionRequest req) {
        int count = clientService.bulkDelete(req);
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @PostMapping("/bulk-activate")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.EDIT)
    public ResponseEntity<Map<String, Integer>> bulkActivate(@Valid @RequestBody BulkActionRequest req) {
        int count = clientService.bulkActivate(req);
        return ResponseEntity.ok(Map.of("activated", count));
    }

    @PostMapping("/bulk-deactivate")
    @RequiresPermission(module = PermissionModule.CLIENTS, action = PermissionAction.EDIT)
    public ResponseEntity<Map<String, Integer>> bulkDeactivate(@Valid @RequestBody BulkActionRequest req) {
        int count = clientService.bulkDeactivate(req);
        return ResponseEntity.ok(Map.of("deactivated", count));
    }
}
