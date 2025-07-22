package energy.eddie.outbound.rest.persistence.specifications;

import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;

public class ConnectionStatusMessageSpecification {
    private ConnectionStatusMessageSpecification() {
        // Utility Class
    }

    public static Specification<ConnectionStatusMessageModel> insertedAfterEquals(ZonedDateTime dateTime) {
        return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("insertedAt"), dateTime);
    }

    public static Specification<ConnectionStatusMessageModel> insertedBeforeEquals(ZonedDateTime dateTime) {
        return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("insertedAt"), dateTime);
    }
}
