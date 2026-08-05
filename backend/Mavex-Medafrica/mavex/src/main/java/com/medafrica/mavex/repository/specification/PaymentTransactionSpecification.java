package com.medafrica.mavex.repository.specification;

import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentTransactionSpecification {

    public static Specification<PaymentTransaction> build(
            String hawb,
            String client,
            PaymentGatewayType gateway,
            PaymentStatus status,
            BigDecimal amountMin,
            BigDecimal amountMax,
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean joined = false;

            // Colonnes Order.hawb / Order.client.fullName — un seul JOIN partagé
            boolean needsOrder = (hawb != null && !hawb.isBlank())
                    || (client != null && !client.isBlank());

            if (needsOrder) {
                Join<Object, Object> o = root.join("order", JoinType.LEFT);
                joined = true;

                if (hawb != null && !hawb.isBlank()) {
                    predicates.add(cb.like(
                        cb.lower(o.get("hawb")),
                        "%" + hawb.trim().toLowerCase() + "%"
                    ));
                }

                if (client != null && !client.isBlank()) {
                    Join<Object, Object> c = o.join("client", JoinType.LEFT);
                    predicates.add(cb.like(
                        cb.lower(c.get("fullName")),
                        "%" + client.trim().toLowerCase() + "%"
                    ));
                }
            }

            if (gateway != null) {
                predicates.add(cb.equal(root.get("gateway"), gateway));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (amountMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("amount"), amountMin));
            }

            if (amountMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("amount"), amountMax));
            }

            // Dates — createdAt est un LocalDateTime : on le tronque en DATE
            // pour comparer à des LocalDate (meme pattern que OrderSpecification)
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

            if (joined) {
                query.distinct(true);
            }

            // Fetch join des relations utilisées systématiquement par PaymentTransactionResponse
            // (order.hawb, order.client.fullName). Exclu de la requête COUNT auto-générée
            // par Spring Data (getResultType() == Long/long), sinon Hibernate lève une
            // IllegalArgumentException (fetch interdit sur un COUNT). Fusionne automatiquement
            // avec les join() conditionnels ci-dessus s'ils portent sur le même attribut en LEFT.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("order", JoinType.LEFT).fetch("client", JoinType.LEFT);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
