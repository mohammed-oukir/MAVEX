package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> build(
            String hawb,
            String client,
            String clientEmail,
            String shipmentSearch,
            Double shipmentWeight,
            Double customsValue,
            Double totalAmount,
            Double dutyRate,
            String customsCurrency,
            OrderStatus status,
            Long shipmentId,
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean joined = false;

            // Colonne HAWB — hawb OU référence alternative
            if (hawb != null && !hawb.isBlank()) {
                String pattern = "%" + hawb.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("hawb")),               pattern),
                    cb.like(cb.lower(root.get("alternateReference")), pattern)
                ));
            }

            // Colonnes Client et Email — un seul LEFT JOIN partagé (relation nullable)
            boolean needsClient = (client != null && !client.isBlank()) 
                    || (clientEmail != null && !clientEmail.isBlank());

            if (needsClient) {
                Join<Object, Object> c = root.join("client", JoinType.LEFT);
                joined = true;

                if (client != null && !client.isBlank()) {
                    predicates.add(cb.like(
                        cb.lower(c.get("fullName")),
                        "%" + client.trim().toLowerCase() + "%"
                    ));
                }

                if (clientEmail != null && !clientEmail.isBlank()) {
                    predicates.add(cb.like(
                        cb.lower(c.get("email")),
                        "%" + clientEmail.trim().toLowerCase() + "%"
                    ));
                }
            }

            // Colonne Shipment — MAWB OU nom du shipper (deux LEFT JOIN chaînés)
            if (shipmentSearch != null && !shipmentSearch.isBlank()) {
                String pattern = "%" + shipmentSearch.trim().toLowerCase() + "%";
                Join<Object, Object> s  = root.join("shipment", JoinType.LEFT);
                Join<Object, Object> sh = s.join("shipper", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(s.get("mawb")),         pattern),
                    cb.like(cb.lower(sh.get("companyName")), pattern)
                ));
                joined = true;
            }

            // Colonnes numériques — tous ces champs sont des BigDecimal
            if (shipmentWeight != null) {
                predicates.add(cb.equal(root.<BigDecimal>get("shipmentWeight"),
                        BigDecimal.valueOf(shipmentWeight)));
            }

            if (customsValue != null) {
                predicates.add(cb.equal(root.<BigDecimal>get("customsValue"),
                        BigDecimal.valueOf(customsValue)));
            }

            if (totalAmount != null) {
                predicates.add(cb.equal(root.<BigDecimal>get("totalAmount"),
                        BigDecimal.valueOf(totalAmount)));
            }

            if (dutyRate != null) {
                predicates.add(cb.equal(root.<BigDecimal>get("dutyRate"),
                        BigDecimal.valueOf(dutyRate)));
            }

            // Colonne Devise
            if (customsCurrency != null && !customsCurrency.isBlank()) {
                predicates.add(cb.equal(root.get("customsCurrency"), customsCurrency));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (shipmentId != null) {
                predicates.add(cb.equal(root.get("shipment").get("id"), shipmentId));
            }

            // Dates — createdAt est un LocalDateTime : on le tronque en DATE
            // pour comparer à des LocalDate (évite atStartOfDay()/atTime(23,59,59))
            if (from != null || to != null) {
                Expression<LocalDate> createdAtAsDate =
                        cb.function("DATE", LocalDate.class, root.get("createdAt"));

                if (from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(createdAtAsDate, from));
                }
                if (to != null) {
                    predicates.add(cb.lessThanOrEqualTo(createdAtAsDate, to));
                }
            }

            // Évite les doublons causés par les LEFT JOIN
            if (joined) {
                query.distinct(true);
            }

            // Fetch join des relations utilisées systématiquement par OrderResponse
            // (client.fullName, shipment.mawb, shipment.shipper.companyName).
            // Exclu de la requête COUNT auto-générée par Spring Data (getResultType() == Long/long),
            // sinon Hibernate lève une IllegalArgumentException (fetch interdit sur un COUNT).
            // Hibernate fusionne automatiquement avec les join() conditionnels ci-dessus
            // s'ils portent sur le même attribut en LEFT — pas de double jointure SQL.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("client", JoinType.LEFT);
                root.fetch("shipment", JoinType.LEFT).fetch("shipper", JoinType.LEFT);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
