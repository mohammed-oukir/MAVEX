package com.medafrica.mavex.dto.permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Corps de la requête PUT /api/permissions/users/{userId} */
@Data
public class SavePermissionsRequest {

    @NotNull
    @Valid
    private List<PermissionEntryDTO> permissions;
}
