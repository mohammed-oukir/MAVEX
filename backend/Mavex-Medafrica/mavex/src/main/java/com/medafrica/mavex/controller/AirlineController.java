package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.airline.AirlineDTO;
import com.medafrica.mavex.model.logistics.Airline;
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
public class AirlineController {

    private final AirlineServiceImpl airlineService;

    /** Gestion Airlines — nécessite VIEW sur le module Airlines */
    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('AIRLINES','VIEW')")
    public ResponseEntity<List<Airline>> getAll() {
        return ResponseEntity.ok(airlineService.findAll());
    }

    /**
     * Lookup par préfixe MAWB — utilisé dans les formulaires d'autres modules.
     * Exempté de la permission Airlines (protégé uniquement par JWT — tout utilisateur authentifié).
     */
    @GetMapping("/prefix/{mawb}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Airline> getByMawb(@PathVariable String mawb) {
        return airlineService.findByPrefix(mawb)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recherche par nom — utilisée dans les formulaires d'autres modules.
     * Exempté de la permission Airlines (protégé uniquement par JWT — tout utilisateur authentifié).
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Airline>> search(@RequestParam String name) {
        return ResponseEntity.ok(airlineService.search(name));
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('AIRLINES','CREATE')")
    public ResponseEntity<Airline> create(@Valid @RequestBody AirlineDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.create(dto));
    }

    @PutMapping("/{prefix}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('AIRLINES','UPDATE')")
    public ResponseEntity<Airline> update(@PathVariable String prefix, @Valid @RequestBody AirlineDTO dto) {
        return ResponseEntity.ok(airlineService.update(prefix, dto));
    }

    @DeleteMapping("/{prefix}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('AIRLINES','DELETE')")
    public ResponseEntity<Void> delete(@PathVariable String prefix) {
        airlineService.delete(prefix);
        return ResponseEntity.noContent().build();
    }
}
