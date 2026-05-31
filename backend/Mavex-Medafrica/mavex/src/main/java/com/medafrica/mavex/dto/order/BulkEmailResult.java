package com.medafrica.mavex.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkEmailResult {
    private int total;
    private int sent;
    private int failed;
}
