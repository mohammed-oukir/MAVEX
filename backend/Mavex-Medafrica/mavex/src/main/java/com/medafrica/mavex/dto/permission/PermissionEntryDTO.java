package com.medafrica.mavex.dto.permission;

import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Une permission individuelle : module + action + accordée ou non. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PermissionEntryDTO {
    private PermissionModule module;
    private PermissionAction action;
    private boolean granted;
}
