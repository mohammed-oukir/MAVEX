package com.medafrica.mavex.security.aspect;

import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Intercepte chaque méthode annotée avec @RequiresPermission.
 * Vérifie en base (via PermissionService) que l'utilisateur connecté
 * a bien la permission requise. Jamais basé sur le contenu du JWT.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }

        if (!(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Principal invalide");
        }

        boolean allowed = permissionService.hasPermission(
                user.getId(),
                requiresPermission.module(),
                requiresPermission.action()
        );

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Permission refusée : " + requiresPermission.module() + " / " + requiresPermission.action()
            );
        }
    }
}
