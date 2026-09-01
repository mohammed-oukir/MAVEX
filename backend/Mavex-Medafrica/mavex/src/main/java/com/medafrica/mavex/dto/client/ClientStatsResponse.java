package com.medafrica.mavex.dto.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientStatsResponse {
    private long totalClients;
    private long activeClients;
    private long inactiveClients;
    private long newThisMonth;
}
