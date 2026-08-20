package com.medafrica.mavex.service;

import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.AgentPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Evalue si l'utilisateur actuellement authentifie possede une permission
 * precise (module + action). Destine a etre utilise plus tard dans des
 * @PreAuthorize sur les controllers (ex: @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','CREATE')")).
 *
 * Regles :
 *  - ADMIN            -> toujours autorise
 *  - AGENT             -> autorise seulement si une ligne AgentPermission existe pour ce (module, action)
 *  - tout autre role   -> refuse
 *  - pas d'utilisateur authentifie (cas anormal) -> refuse (fail-safe)
 */
@Service
@RequiredArgsConstructor
public class PermissionEvaluatorService {

    private final AgentPermissionRepository agentPermissionRepository;

    public boolean hasPermission(String module, String action) {
        User user = currentUser();
        if (user == null) {
            return false;
        }

        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        if (user.getRole() == UserRole.AGENT) {
            return agentPermissionRepository
                    .existsByAgent_IdAndPermission_ModuleAndPermission_Action(user.getId(), module, action);
        }

        return false;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
