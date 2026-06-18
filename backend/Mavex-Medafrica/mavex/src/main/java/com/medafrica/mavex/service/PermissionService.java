package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.permission.*;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.security.AgentPermission;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.AgentPermissionRepository;
import com.medafrica.mavex.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final AgentPermissionRepository permissionRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────
    // Vérification d'une permission (appelée par @RequiresPermission)
    // ─────────────────────────────────────────────────────────────

    /**
     * Vérifie en base si l'utilisateur a la permission accordée.
     * Les ADMIN ont toujours accès — sans consulter la base.
     * Cette méthode est le point central de sécurité : elle est appelée
     * à chaque requête et ne se base jamais sur le JWT.
     */
    public boolean hasPermission(Long userId, PermissionModule module, PermissionAction action) {
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null || !user.isActive()) return false;

        if (user.getRole() == UserRole.ADMIN) return true;

        return permissionRepository
                .findByUserIdAndModuleAndAction(userId, module, action)
                .map(AgentPermission::isGranted)
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────
    // Lecture des permissions d'un agent
    // ─────────────────────────────────────────────────────────────

    public UserPermissionsDTO getPermissionsForUser(Long userId) {
        User user = findAgent(userId);
        List<AgentPermission> stored = permissionRepository.findByUserId(userId);

        List<PermissionEntryDTO> entries = buildFullEntryList(stored);
        Map<PermissionModule, Set<String>> byModule = buildByModuleMap(entries);

        return UserPermissionsDTO.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .entries(entries)
                .byModule(byModule)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Sauvegarde des permissions d'un agent
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public UserPermissionsDTO savePermissions(Long userId, List<PermissionEntryDTO> incoming) {
        User user = findAgent(userId);

        List<PermissionEntryDTO> validated = applyCascadeRules(incoming);

        permissionRepository.deleteAllByUserId(userId);

        List<AgentPermission> toSave = validated.stream()
                .map(entry -> AgentPermission.builder()
                        .user(user)
                        .module(entry.getModule())
                        .action(entry.getAction())
                        .granted(entry.isGranted())
                        .build())
                .toList();

        permissionRepository.saveAll(toSave);

        return getPermissionsForUser(userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Liste de tous les agents avec résumé
    // ─────────────────────────────────────────────────────────────

    public List<AgentSummaryDTO> getAllAgentsSummary() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.AGENT && u.isActive())
                .map(u -> {
                    long granted = permissionRepository.findByUserId(u.getId()).stream()
                            .filter(AgentPermission::isGranted)
                            .count();
                    return AgentSummaryDTO.builder()
                            .userId(u.getId())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .totalGranted((int) granted)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // Permissions effectives pour le frontend (réponse login)
    // ─────────────────────────────────────────────────────────────

    /**
     * Retourne la map module → liste d'actions accordées.
     * Utilisé dans la réponse de login pour que le frontend sache
     * quoi afficher. Ne sert PAS à la sécurité réelle.
     */
    public Map<PermissionModule, Set<String>> getEffectivePermissionsMap(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return buildAdminFullMap();
        }
        List<AgentPermission> stored = permissionRepository.findByUserId(user.getId());
        List<PermissionEntryDTO> entries = buildFullEntryList(stored);
        return buildByModuleMap(entries);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────

    private User findAgent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userId));
        if (user.getRole() != UserRole.AGENT) {
            throw new IllegalArgumentException("Les permissions individuelles ne s'appliquent qu'aux agents.");
        }
        return user;
    }

    /**
     * Règle de cascade backend :
     * - Si CREATE, EDIT ou DELETE est granted → VIEW est forcé granted.
     * - Si VIEW est false → CREATE, EDIT, DELETE sont forcés false.
     */
    private List<PermissionEntryDTO> applyCascadeRules(List<PermissionEntryDTO> incoming) {
        Map<PermissionModule, Map<PermissionAction, Boolean>> byModule = new EnumMap<>(PermissionModule.class);

        for (PermissionEntryDTO entry : incoming) {
            byModule
                .computeIfAbsent(entry.getModule(), m -> new EnumMap<>(PermissionAction.class))
                .put(entry.getAction(), entry.isGranted());
        }

        List<PermissionEntryDTO> result = new ArrayList<>();

        for (PermissionModule module : PermissionModule.values()) {
            Map<PermissionAction, Boolean> actions = byModule.getOrDefault(module, new EnumMap<>(PermissionAction.class));

            boolean view   = Boolean.TRUE.equals(actions.get(PermissionAction.VIEW));
            boolean create = Boolean.TRUE.equals(actions.get(PermissionAction.CREATE));
            boolean edit   = Boolean.TRUE.equals(actions.get(PermissionAction.EDIT));
            boolean delete = Boolean.TRUE.equals(actions.get(PermissionAction.DELETE));

            // Cascade montante : si une action nécessite VIEW, VIEW est forcé true
            if (create || edit || delete) view = true;

            // Cascade descendante : si VIEW est retiré, tout est retiré
            if (!view) { create = false; edit = false; delete = false; }

            result.add(new PermissionEntryDTO(module, PermissionAction.VIEW,   view));
            result.add(new PermissionEntryDTO(module, PermissionAction.CREATE, create));
            result.add(new PermissionEntryDTO(module, PermissionAction.EDIT,   edit));
            result.add(new PermissionEntryDTO(module, PermissionAction.DELETE, delete));
        }

        return result;
    }

    /**
     * Construit la liste complète de toutes les combinaisons module×action
     * en partant des enregistrements stockés (les absents = false).
     */
    private List<PermissionEntryDTO> buildFullEntryList(List<AgentPermission> stored) {
        Map<String, Boolean> index = stored.stream().collect(
                Collectors.toMap(
                        p -> p.getModule() + "_" + p.getAction(),
                        AgentPermission::isGranted
                )
        );

        List<PermissionEntryDTO> full = new ArrayList<>();
        for (PermissionModule module : PermissionModule.values()) {
            for (PermissionAction action : PermissionAction.values()) {
                boolean granted = Boolean.TRUE.equals(index.get(module + "_" + action));
                full.add(new PermissionEntryDTO(module, action, granted));
            }
        }
        return full;
    }

    private Map<PermissionModule, Set<String>> buildByModuleMap(List<PermissionEntryDTO> entries) {
        Map<PermissionModule, Set<String>> map = new EnumMap<>(PermissionModule.class);
        for (PermissionModule m : PermissionModule.values()) {
            map.put(m, new LinkedHashSet<>());
        }
        for (PermissionEntryDTO e : entries) {
            if (e.isGranted()) {
                map.get(e.getModule()).add(e.getAction().name());
            }
        }
        return map;
    }

    private Map<PermissionModule, Set<String>> buildAdminFullMap() {
        Map<PermissionModule, Set<String>> map = new EnumMap<>(PermissionModule.class);
        Set<String> allActions = Set.of("VIEW", "CREATE", "EDIT", "DELETE");
        for (PermissionModule m : PermissionModule.values()) {
            map.put(m, allActions);
        }
        return map;
    }
}
