package com.medafrica.mavex.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkStatusResult {
    private int total;
    private int succeeded;
    private int failed;
}
