package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.client.ClientPatchRequest;
import com.medafrica.mavex.dto.client.ClientRequestDTO;
import com.medafrica.mavex.dto.client.ClientResponseDTO;
import com.medafrica.mavex.service.interfaces.ClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    /** GET /api/clients */
    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> getAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    /** GET /api/clients/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    /** POST /api/clients */
    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(@Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }

    /** PUT /api/clients/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }
/** PATCH /api/clients/{id} */
@PatchMapping("/{id}")
public ResponseEntity<ClientResponseDTO> patch(@PathVariable Long id,
                                               @Valid @RequestBody ClientPatchRequest dto) {
    return ResponseEntity.ok(clientService.patch(id, dto));
}

    /** DELETE /api/clients/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}