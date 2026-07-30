package com.medafrica.mavex.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailStatsSummaryDto {

    private int totalSent;
    private int totalDelivered;
    private int totalOpened;
    private int totalBounced;
    private double deliveryRate;
    private double openRate;
}
