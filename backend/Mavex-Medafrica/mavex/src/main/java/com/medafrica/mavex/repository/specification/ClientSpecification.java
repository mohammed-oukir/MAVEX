package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.dto.client.ClientSearchCriteria;
import com.medafrica.mavex.model.actor.Client;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ClientSpecification {

    public static Specification<Client> build(ClientSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getName() != null && !criteria.getName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("fullName")),
                        "%" + criteria.getName().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + criteria.getEmail().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getPhone() != null && !criteria.getPhone().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("phone")),
                        "%" + criteria.getPhone().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getCity() != null && !criteria.getCity().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("city")),
                        "%" + criteria.getCity().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getState() != null && !criteria.getState().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("state")),
                        "%" + criteria.getState().trim().toLowerCase() + "%"
                ));
            }

            // country : code OU name (OR interne), combiné en AND avec les autres critères
            if (criteria.getCountry() != null && !criteria.getCountry().isBlank()) {
                Join<Object, Object> country = root.join("country", JoinType.LEFT);
                String pattern = "%" + criteria.getCountry().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(country.get("code")), pattern),
                        cb.like(cb.lower(country.get("name")), pattern)
                ));
            }

            if (criteria.getStatus() != null && !criteria.getStatus().isBlank()
                    && !"all".equalsIgnoreCase(criteria.getStatus())) {
                boolean active = "active".equalsIgnoreCase(criteria.getStatus());
                predicates.add(cb.equal(root.get("active"), active));
            }

            // Range createdAt — bornes explicites 00:00:00 / 23:59:59.999999999
            // (équivalent du filtre JS actuel : to.setHours(23, 59, 59, 999)),
            // sans appliquer de fonction sur la colonne pour rester sargable.
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), criteria.getDateFrom().atStartOfDay()
                ));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), criteria.getDateTo().atTime(LocalTime.MAX)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
