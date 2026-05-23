package com.medafrica.mavex.model.country;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @NotBlank(message = "Le code pays est obligatoire")
    @Size(min = 2, max = 2, message = "Le code pays doit faire 2 caractères")
    private String code;   // US, MA, FR

    @NotBlank(message = "Le nom du pays est obligatoire")
    private String name;   // Un    ited States, Morocco, France
}