package com.medafrica.mavex.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailStatsDayDto {

    private LocalDate date;
    private int sent;
    private int delivered;
    private int opened;
    private int bounced;
}
