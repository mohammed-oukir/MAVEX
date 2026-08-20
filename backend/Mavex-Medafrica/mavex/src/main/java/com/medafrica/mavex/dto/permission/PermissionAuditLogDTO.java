package com.medafrica.mavex.dto.permission;

import com.medafrica.mavex.model.enums.PermissionActionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionAuditLogDTO {

    private Long id;
    private String permissionModule;
    private String permissionAction;
    private String permissionLabel;
    private PermissionActionType actionType;
    private String performedByName;
    private LocalDateTime performedAt;
}
