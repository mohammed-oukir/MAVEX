package com.medafrica.mavex.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long adminCount;
    private long agentCount;
    private long comptableCount;
}
