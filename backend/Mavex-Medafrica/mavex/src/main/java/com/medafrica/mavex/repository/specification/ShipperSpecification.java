package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.dto.shipper.ShipperSearchCriteria;
import com.medafrica.mavex.model.actor.Shipper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ShipperSpecification {

    public static Specification<Shipper> build(ShipperSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getCompany() != null && !criteria.getCompany().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("companyName")),
                        "%" + criteria.getCompany().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getContact() != null && !criteria.getContact().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("contactName")),
                        "%" + criteria.getContact().trim().toLowerCase() + "%"
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

            // country : test sur code uniquement (pas de name), LEFT JOIN seulement si rempli
            if (criteria.getCountry() != null && !criteria.getCountry().isBlank()) {
                Join<Object, Object> country = root.join("country", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(country.get("code")),
                        "%" + criteria.getCountry().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getStatus() != null && !criteria.getStatus().isBlank()
                    && !"all".equalsIgnoreCase(criteria.getStatus())) {
                boolean active = "active".equalsIgnoreCase(criteria.getStatus());
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
