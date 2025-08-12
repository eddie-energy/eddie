package energy.eddie.outbound.rest.persistence.specifications;

import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;

public class InsertionTimeSpecification {
    private InsertionTimeSpecification() {
        // Utility Class
    }

    public static <T> Specification<T> insertedAfterEquals(ZonedDateTime dateTime) {
        return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("insertedAt"), dateTime);
    }

    public static <T> Specification<T> insertedBeforeEquals(ZonedDateTime dateTime) {
        return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("insertedAt"), dateTime);
    }
}
