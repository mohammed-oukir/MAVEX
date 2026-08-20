package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.permission.AgentPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentPermissionRepository extends JpaRepository<AgentPermission, Long> {

    boolean existsByAgent_IdAndPermission_ModuleAndPermission_Action(
            Long agentId, String module, String action);

    List<AgentPermission> findByAgent_Id(Long agentId);
}
