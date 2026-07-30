package com.medafrica.mavex.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrevoQuotaResponse {

    private Integer remainingCredits;
    private boolean available;
    private String  errorMessage;
}
