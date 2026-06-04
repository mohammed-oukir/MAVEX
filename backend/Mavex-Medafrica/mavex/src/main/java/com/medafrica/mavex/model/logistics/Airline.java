package com.medafrica.mavex.model.logistics;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airlines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Airline {

    @Id
    @Column(name = "prefix", length = 3)
    private String prefix;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "iata_code", length = 2)
    private String iataCode;

    @Column(name = "icao_code", length = 3)
    private String icaoCode;

    @Column(name = "country")
    private String country;

    @Column(name = "mode", length = 10)
    @Builder.Default
    private String mode = "AIR";
}
