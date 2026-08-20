package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.NotificationTypeDTO;
import com.medafrica.mavex.service.interfaces.NotificationTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification-types")
@RequiredArgsConstructor
public class NotificationTypeController {

    private final NotificationTypeService notificationTypeService;

    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','VIEW_NOTIFICATION_TYPES')")
    public ResponseEntity<List<NotificationTypeDTO>> getAll() {
        return ResponseEntity.ok(notificationTypeService.getAll());
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','CREATE_NOTIFICATION_TYPE')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body("Le champ 'name' est obligatoire.");
        }
        return ResponseEntity.ok(notificationTypeService.create(name));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','UPDATE_NOTIFICATION_TYPE')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body("Le champ 'name' est obligatoire.");
        }
        return ResponseEntity.ok(notificationTypeService.update(id, name));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','DELETE_NOTIFICATION_TYPE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
