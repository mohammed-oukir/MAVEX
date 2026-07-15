package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.email.NotificationTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationTypeEntity, Long> {

    Optional<NotificationTypeEntity> findByName(String name);

    boolean existsByName(String name);
}
