package com.medafrica.mavex.model.actor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import com.medafrica.mavex.model.country.Country;

/**
 * Destinataire americain de la marchandise.
 * Recoit email facture et effectue le paiement.
 *
 * Colonnes Excel mappees :
 *   Receiver Name       -> fullName
 *   Receiver Email      -> email
 *   Receiver Phone      -> phone
 *   Receiver Address 1  -> address
 *   Receiver Location   -> city
 *   Receiver State      -> state
 *   Receiver Postcode   -> zipCode
 *   Receiver Country    -> country
 *
 * Table BDD : clients
 */
@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Receptionne la facture et le lien de paiement */
    @Column(nullable = false)
    private String email;

    private String phone;
    private String address;
    private String city;

    /** Code etat US sur 2 lettres (NY, CA, CO...) */
    @Column(length = 2)
    private String state;

    /** Code postal US - String pour garder les zeros de gauche */
    @Column(name = "zip_code")
    private String zipCode;

   @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "country_code", nullable = false)
private Country country;
   /*@Builder.Default
    private String country = "US"; */ 
   

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    /** false = compte désactivé (soft delete) */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

}