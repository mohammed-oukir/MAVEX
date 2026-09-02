package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.model.enums.DutyChangeEntityType;
import com.medafrica.mavex.model.logistics.DutyChangeHistory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DutyChangeHistorySpecification {

    /** Historique SHIPMENT d'un shipment donné (nom de l'agent + plage de dates). */
    public static Specification<DutyChangeHistory> forShipment(
            Long shipmentId, String changedByName, LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("entityType"), DutyChangeEntityType.SHIPMENT));
            predicates.add(cb.equal(root.get("shipment").get("id"), shipmentId));

            if (changedByName != null && !changedByName.isBlank()) {
                Join<Object, Object> u = root.join("changedBy", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(u.get("fullName")),
                        "%" + changedByName.trim().toLowerCase() + "%"
                ));
            }

            addDateRange(predicates, cb, root, from, to);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Historique ORDER de tous les orders rattachés à un shipment donné (HAWB + nom + plage de dates). */
    public static Specification<DutyChangeHistory> forShipmentOrders(
            Long shipmentId, String hawb, String changedByName, LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("entityType"), DutyChangeEntityType.ORDER));
            predicates.add(cb.equal(root.get("order").get("shipment").get("id"), shipmentId));

            if (hawb != null && !hawb.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("order").get("hawb")),
                        "%" + hawb.trim().toLowerCase() + "%"
                ));
            }

            if (changedByName != null && !changedByName.isBlank()) {
                Join<Object, Object> u = root.join("changedBy", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(u.get("fullName")),
                        "%" + changedByName.trim().toLowerCase() + "%"
                ));
            }

            addDateRange(predicates, cb, root, from, to);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addDateRange(List<Predicate> predicates, CriteriaBuilder cb,
                                      Root<DutyChangeHistory> root,
                                      LocalDate from, LocalDate to) {
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("changedAt"), from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("changedAt"), LocalDateTime.of(to, LocalTime.MAX)));
        }
    }
}
