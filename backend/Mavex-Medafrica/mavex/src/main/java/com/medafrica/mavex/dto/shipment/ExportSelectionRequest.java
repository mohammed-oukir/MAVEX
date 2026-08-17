package com.medafrica.mavex.dto.shipment;

import lombok.Data;
import java.util.List;

@Data
public class ExportSelectionRequest {
    private List<Long> ids;
}
