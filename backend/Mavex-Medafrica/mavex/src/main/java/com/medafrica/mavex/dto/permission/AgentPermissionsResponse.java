package com.medafrica.mavex.dto.permission;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentPermissionsResponse {

    private Long agentId;
    private String agentName;
    private List<Long> permissionIds;
}
