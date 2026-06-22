package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.notification.Notification;
import com.medafrica.mavex.model.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);

    List<Notification> findByUser_RoleAndReadFalseOrderByCreatedAtDesc(UserRole role);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.read = false")
    void markAllReadByUser(@Param("user") User user);
}
