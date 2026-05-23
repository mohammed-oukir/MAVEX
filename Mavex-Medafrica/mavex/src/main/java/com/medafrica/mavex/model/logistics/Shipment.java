package com.medafrica.mavex.model.logistics;

import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Envoi groupe identifie par le MAWB (Master Air Waybill).
 * Contient plusieurs Orders (HAWB).
 * Cree lors de l'import du manifeste Excel.
 *
 * Colonne Excel "Mawb #" -> mawb (ex: 147-44480365)
 * Table BDD : shipments
 */
@Entity
@Table(name = "shipments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Colonne Excel "Mawb #" ex: 147-44480365 */
    @Column(unique = true, nullable = false)       
    private String mawb;

    @Column(name = "export_date")
    private LocalDate exportDate;

    @Column(name = "import_date")
    private LocalDate importDate;

    /** Compagnie aerienne ex: AT (Air Transat) */
    @Column(name = "importing_carrier")
    private String importingCarrier;

    /** Mode CBP ex: 40 = Air */
    @Column(name = "mode_of_transport")
    private String modeOfTransport;

    /** Port CBP ex: 4774 = JFK New York */
    @Column(name = "port_code")
    private String portCode;
    



    /** DRAFT -> IMPORTED -> PROCESSING -> CLOSED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.DRAFT;

    /** FK -> shippers.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    private Shipper shipper;

    /** FK -> users.id - agent qui a importe */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Liste des Orders (HAWB) de ce shipment */
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


