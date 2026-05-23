package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.ApiResponse;
import com.medafrica.mavex.dto.user.UserRequestDTO;
import com.medafrica.mavex.dto.user.UserResponseDTO;
import com.medafrica.mavex.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users */
    /**  @PreAuthorize("hasRole('ADMIN')")*/
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /** GET /api/users/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /** POST /api/users */
   /** POST /api/users */
@PostMapping
public ResponseEntity<ApiResponse<UserResponseDTO>> create(@Valid @RequestBody UserRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
        ApiResponse.<UserResponseDTO>builder()
            .message("Utilisateur créé avec succès")
            .data(userService.create(dto))
            .build()
    );
}

/** PUT /api/users/{id} */
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponseDTO>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UserRequestDTO dto) {
    return ResponseEntity.ok(
        ApiResponse.<UserResponseDTO>builder()
            .message("Utilisateur modifié avec succès")
            .data(userService.update(id, dto))
            .build()
    );
}

/** PATCH /api/users/{id} */
@PatchMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponseDTO>> patch(@PathVariable Long id,
                                                           @RequestBody UserRequestDTO dto) {
    return ResponseEntity.ok(
        ApiResponse.<UserResponseDTO>builder()
            .message("Utilisateur mis à jour partiellement avec succès")
            .data(userService.patch(id, dto))
            .build()
    );
}

/** DELETE /api/users/{id} */
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .message("Utilisateur désactivé avec succès")
            .data(null)
            .build()
    );

}

@PatchMapping("/{id}/activate")
public ResponseEntity<Void> activate(@PathVariable Long id) {
    userService.activate(id);
    return ResponseEntity.noContent().build();
}

}