package com.medafrica.mavex.dto.shipper;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.medafrica.mavex.model.country.Country;

@Data
@Builder
public class ShipperResponseDTO {

    private Long id;
    private String companyName;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private String city;

    /** Code ISO pays ex: MA */
    private Country countryCode;

    private boolean active;
    private LocalDateTime createdAt;
}