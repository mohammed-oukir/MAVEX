package com.medafrica.mavex.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class BulkEmailRequest {
    private List<Long> ids;
}
