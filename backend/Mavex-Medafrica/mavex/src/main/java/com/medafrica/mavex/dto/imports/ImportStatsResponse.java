package com.medafrica.mavex.dto.imports;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportStatsResponse {
    private long importsThisMonth;
    private double successRatePercent;
    private long failedRowsRecent;
    private long totalImportsRecent;
}
