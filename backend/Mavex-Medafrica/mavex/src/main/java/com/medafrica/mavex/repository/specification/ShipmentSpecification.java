package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.model.logistics.Shipment;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShipmentSpecification {

    public static Specification<Shipment> build(
            String mawb,
            String shipperCompanyName,
            LocalDate importFrom,
            LocalDate importTo,
            String carrier,
            String mode,
            Integer totalOrders,
            Double dutyRateMin,
            Double dutyRateMax,
            ShipmentStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Colonne MAWB
            if (mawb != null && !mawb.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("mawb")),
                    "%" + mawb.trim().toLowerCase() + "%"
                ));
            }

            // Filtre Shipper — LEFT JOIN car la relation est nullable
            if (shipperCompanyName != null && !shipperCompanyName.isBlank()) {
                Join<Object, Object> shipper = root.join("shipper", JoinType.LEFT);
                predicates.add(cb.like(
                    cb.lower(shipper.get("companyName")),
                    "%" + shipperCompanyName.trim().toLowerCase() + "%"
                ));
                // Évite les doublons causés par le LEFT JOIN
                query.distinct(true);
            }

            // Colonne Date arrivée — importDate, avec repli sur createdAt
            // (même logique que l'affichage frontend).
            // createdAt est un LocalDateTime : on le tronque en DATE pour que
            // les deux branches du coalesce soient homogènes.
            Expression<LocalDate> createdAtAsDate =
                    cb.function("DATE", LocalDate.class, root.get("createdAt"));
            Expression<LocalDate> effectiveDate =
                    cb.coalesce(root.get("importDate"), createdAtAsDate);

            if (importFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(effectiveDate, importFrom));
            }

            if (importTo != null) {
                predicates.add(cb.lessThanOrEqualTo(effectiveDate, importTo));
            }

            // Colonne Carrier
            if (carrier != null && !carrier.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("importingCarrier")),
                    "%" + carrier.trim().toLowerCase() + "%"
                ));
            }

            // Colonne Mode
            if (mode != null && !mode.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("modeOfTransport")),
                    "%" + mode.trim().toLowerCase() + "%"
                ));
            }

            // Colonne Orders — totalOrders n'est pas un champ mappé :
            // c'est la taille de la collection "orders" (cf. ShipmentServiceImpl)
            if (totalOrders != null) {
                predicates.add(cb.equal(cb.size(root.get("orders")), totalOrders));
            }

            // Colonne Duty — dutyRate est un BigDecimal
            if (dutyRateMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                    root.<BigDecimal>get("dutyRate"), BigDecimal.valueOf(dutyRateMin)
                ));
            }

            if (dutyRateMax != null) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.<BigDecimal>get("dutyRate"), BigDecimal.valueOf(dutyRateMax)
                ));
            }

            // Colonne Statut
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}