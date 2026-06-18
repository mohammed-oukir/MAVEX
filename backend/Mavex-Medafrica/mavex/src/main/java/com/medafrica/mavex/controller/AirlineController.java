package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.airline.AirlineDTO;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.model.logistics.Airline;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.AirlineServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class AirlineController {

    private final AirlineServiceImpl airlineService;

    /** Gestion Airlines — nécessite VIEW sur le module Airlines */
    @GetMapping
    @RequiresPermission(module = PermissionModule.AIRLINES, action = PermissionAction.VIEW)
    public ResponseEntity<List<Airline>> getAll() {
        return ResponseEntity.ok(airlineService.findAll());
    }

    /**
     * Lookup par préfixe MAWB — utilisé dans les formulaires d'autres modules.
     * Option A : exempté de la permission Airlines (protégé uniquement par JWT).
     */
    @GetMapping("/prefix/{mawb}")
    public ResponseEntity<Airline> getByMawb(@PathVariable String mawb) {
        return airlineService.findByPrefix(mawb)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recherche par nom — utilisée dans les formulaires d'autres modules.
     * Option A : exempté de la permission Airlines (protégé uniquement par JWT).
     */
    @GetMapping("/search")
    public ResponseEntity<List<Airline>> search(@RequestParam String name) {
        return ResponseEntity.ok(airlineService.search(name));
    }

    @PostMapping
    @RequiresPermission(module = PermissionModule.AIRLINES, action = PermissionAction.CREATE)
    public ResponseEntity<Airline> create(@Valid @RequestBody AirlineDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.create(dto));
    }

    @PutMapping("/{prefix}")
    @RequiresPermission(module = PermissionModule.AIRLINES, action = PermissionAction.EDIT)
    public ResponseEntity<Airline> update(@PathVariable String prefix, @Valid @RequestBody AirlineDTO dto) {
        return ResponseEntity.ok(airlineService.update(prefix, dto));
    }

    @DeleteMapping("/{prefix}")
    @RequiresPermission(module = PermissionModule.AIRLINES, action = PermissionAction.DELETE)
    public ResponseEntity<Void> delete(@PathVariable String prefix) {
        airlineService.delete(prefix);
        return ResponseEntity.noContent().build();
    }
}
