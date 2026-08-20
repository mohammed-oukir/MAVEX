package com.medafrica.mavex.dto.permission;

import lombok.Data;

import java.util.List;

@Data
public class UpdateAgentPermissionsRequest {

    /** État final souhaité — toutes les permissions que l'agent doit avoir après l'opération. */
    private List<Long> permissionIds;
}
