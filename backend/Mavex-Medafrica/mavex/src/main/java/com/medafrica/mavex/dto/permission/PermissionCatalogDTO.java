package com.medafrica.mavex.dto.permission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionCatalogDTO {

    private Long id;
    private String module;
    private String action;
    private String label;
    private String contextNote;
}
