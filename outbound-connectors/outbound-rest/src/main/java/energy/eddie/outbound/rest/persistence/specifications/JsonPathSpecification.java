// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.PredicateSpecification;

import java.util.ArrayList;
import java.util.List;

public class JsonPathSpecification<T> implements PredicateSpecification<T> {
    private final List<String> path;
    private final String value;

    public JsonPathSpecification(String key, String value) {
        this.path = List.of(key);
        this.value = value;
    }

    public JsonPathSpecification(List<String> path, String value) {
        this.path = path;
        this.value = value;
    }

    @Override
    public @Nullable Predicate toPredicate(@NotNull From<?, T> from, @Nullable CriteriaBuilder cb) {
        if (cb == null) {
            return null;
        }
        var expressions = new ArrayList<Expression<?>>();
        expressions.add(from.get("payload"));
        for (var key : path) {
            expressions.addLast(cb.literal(key));
        }
        return cb.equal(
                cb.function("jsonb_extract_path_text", String.class, expressions.toArray(new Expression<?>[0])),
                value
        );
    }
}
