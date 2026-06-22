package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.notification.NotificationResponse;
import com.medafrica.mavex.model.enums.NotificationLevel;
import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.notification.Notification;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.NotificationRepository;
import com.medafrica.mavex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    @Transactional
    public void createForAllAdmins(String title, String message, String link) {
        List<User> admins = userRepository.findAllByRoleAndActiveTrue(UserRole.ADMIN);
        if (admins.isEmpty()) {
            log.warn("createForAllAdmins — aucun admin actif trouvé, notification non créée.");
            return;
        }
        List<Notification> notifs = admins.stream()
            .map(admin -> Notification.builder()
                .user(admin)
                .level(NotificationLevel.INFO)
                .title(title)
                .message(message)
                .link(link)
                .build())
            .toList();
        notificationRepository.saveAll(notifs);
        log.debug("Notification créée pour {} admin(s) : {}", admins.size(), title);
    }

    public List<NotificationResponse> getUnreadForCurrentUser() {
        return notificationRepository
            .findByUserAndReadFalseOrderByCreatedAtDesc(getCurrentUser())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public long countUnreadForCurrentUser() {
        return notificationRepository.countByUserAndReadFalse(getCurrentUser());
    }

    @Transactional
    public void markAllAsReadForCurrentUser() {
        notificationRepository.markAllReadByUser(getCurrentUser());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .title(n.getTitle())
            .message(n.getMessage())
            .level(n.getLevel())
            .link(n.getLink())
            .read(n.isRead())
            .createdAt(n.getCreatedAt())
            .build();
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User u))
            throw new IllegalStateException("Utilisateur non authentifié.");
        return u;
    }
}
