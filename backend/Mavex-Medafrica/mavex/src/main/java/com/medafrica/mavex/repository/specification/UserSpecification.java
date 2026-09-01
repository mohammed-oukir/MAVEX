package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.dto.user.UserSearchCriteria;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> build(UserSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getFullName() != null && !criteria.getFullName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("fullName")),
                        "%" + criteria.getFullName().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + criteria.getEmail().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.getRole()));
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
