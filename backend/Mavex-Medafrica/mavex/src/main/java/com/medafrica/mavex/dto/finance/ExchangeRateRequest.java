package com.medafrica.mavex.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRateRequest {

    private String     fromCurrency;
    private String     toCurrency;
    private BigDecimal rate;
    private LocalDate  effectiveDate;
    private String     source;
}
