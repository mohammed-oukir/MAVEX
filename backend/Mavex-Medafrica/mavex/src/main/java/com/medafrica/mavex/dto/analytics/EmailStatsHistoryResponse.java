package com.medafrica.mavex.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailStatsHistoryResponse {

    private List<EmailStatsDayDto> days;
    private EmailStatsSummaryDto   summary;
}
