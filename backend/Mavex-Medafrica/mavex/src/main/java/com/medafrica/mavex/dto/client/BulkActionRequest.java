package com.medafrica.mavex.dto.client;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkActionRequest {

    @NotEmpty(message = "La liste des IDs est obligatoire")
    private List<Long> ids;
}
