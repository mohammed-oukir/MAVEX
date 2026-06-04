package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.airline.AirlineDTO;
import com.medafrica.mavex.model.logistics.Airline;
import com.medafrica.mavex.service.AirlineServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineServiceImpl airlineService;

    /** GET /api/airlines */
    @GetMapping
    public ResponseEntity<List<Airline>> getAll() {
        return ResponseEntity.ok(airlineService.findAll());
    }

    /** GET /api/airlines/prefix/{mawb} — accepte le MAWB complet ou juste le préfixe */
    @GetMapping("/prefix/{mawb}")
    public ResponseEntity<Airline> getByMawb(@PathVariable String mawb) {
        return airlineService.findByPrefix(mawb)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/airlines/search?name=royal */
    @GetMapping("/search")
    public ResponseEntity<List<Airline>> search(@RequestParam String name) {
        return ResponseEntity.ok(airlineService.search(name));
    }

    /** POST /api/airlines */
    @PostMapping
    public ResponseEntity<Airline> create(@Valid @RequestBody AirlineDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.create(dto));
    }

    /** PUT /api/airlines/{prefix} */
    @PutMapping("/{prefix}")
    public ResponseEntity<Airline> update(@PathVariable String prefix, @Valid @RequestBody AirlineDTO dto) {
        return ResponseEntity.ok(airlineService.update(prefix, dto));
    }

    /** DELETE /api/airlines/{prefix} */
    @DeleteMapping("/{prefix}")
    public ResponseEntity<Void> delete(@PathVariable String prefix) {
        airlineService.delete(prefix);
        return ResponseEntity.noContent().build();
    }
}
