package com.medafrica.mavex.dto.finance;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRateResponse {

    private Long      id;
    private String    fromCurrency;
    private String    toCurrency;
    private BigDecimal rate;
    private LocalDate effectiveDate;
    private String    source;
    private Boolean   isActive;
    private String    importedByName;
    private LocalDateTime importedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
