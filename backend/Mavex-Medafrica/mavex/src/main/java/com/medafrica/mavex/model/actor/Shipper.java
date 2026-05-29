package com.medafrica.mavex.model.actor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.medafrica.mavex.model.country.Country;

import java.time.LocalDateTime;

/**
 * Expediteur marocain - client de Med Africa Logistics.
 * Colonne Excel "Sender Name" -> companyName (ex: PALAIS D'ART FES)
 * Table BDD : shippers
 */
@Entity
@Table(name = "shippers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Colonne Excel "Sender Name" ex: PALAIS D'ART FES */
    @Column(name = "company_name", nullable = false)
    private String companyName;

    /** Nom du responsable */
    @Column(name = "contact_name")
    private String contactName;

    @Column(nullable = false)
    private String email;

    private String phone;
    private String address;
    private String city;

    /** Code ISO pays - MA par defaut */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;
    
    /** false = archive (soft delete) */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;



/** Colonne Excel "Sender Location Name" */
@Column(name = "location_name")
private String locationName;

/** Colonne Excel "Sender State" */
@Column(length = 10)
private String state;

/** Colonne Excel "Sender Postcode" */
@Column(name = "zip_code")
private String zipCode; 

}