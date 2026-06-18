package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.model.security.AgentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentPermissionRepository extends JpaRepository<AgentPermission, Long> {

    List<AgentPermission> findByUserId(Long userId);

    Optional<AgentPermission> findByUserIdAndModuleAndAction(
            Long userId, PermissionModule module, PermissionAction action);

    @Modifying
    @Query("DELETE FROM AgentPermission p WHERE p.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
