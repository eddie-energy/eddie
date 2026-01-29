package energy.eddie.outbound.rest.persistence.specifications;

import org.springframework.data.jpa.domain.PredicateSpecification;

import java.time.ZonedDateTime;

public class InsertionTimeSpecification {
    private InsertionTimeSpecification() {
        // Utility Class
    }

    public static <T> PredicateSpecification<T> insertedAfterEquals(ZonedDateTime dateTime) {
        return (root, query) ->
                query.greaterThanOrEqualTo(root.get("insertedAt"), dateTime);
    }

    public static <T> PredicateSpecification<T> insertedBeforeEquals(ZonedDateTime dateTime) {
        return (root, query) ->
                query.lessThanOrEqualTo(root.get("insertedAt"), dateTime);
    }
}
