package energy.eddie.outbound.rest.persistence.specifications;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JsonPathSpecification<T> implements Specification<T> {
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
    public Predicate toPredicate(
            Root<T> root,
            CriteriaQuery<?> query,
            @SuppressWarnings("NullableProblems") CriteriaBuilder cb
    ) {
        var expressions = new ArrayList<Expression<?>>();
        expressions.add(root.get("payload"));
        for (var key : path) {
            expressions.addLast(cb.literal(key));
        }
        return cb.equal(
                cb.function("jsonb_extract_path_text", String.class, expressions.toArray(new Expression<?>[0])),
                value
        );
    }
}
