package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> build(
            String q,
            OrderStatus status,
            Long shipmentId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Recherche texte sur HAWB, nom client et email client
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Join<Object, Object> client = root.join("client", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("hawb")),          pattern),
                    cb.like(cb.lower(client.get("fullName")),    pattern),
                    cb.like(cb.lower(client.get("email")),       pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (shipmentId != null) {
                predicates.add(cb.equal(root.get("shipment").get("id"), shipmentId));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            // Évite les doublons causés par le LEFT JOIN sur client
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
