package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

public class NotOlderThanValidator implements Validator<CreatedEvent> {
    private final ChronoUnit unit;

    private final long limit;

    public NotOlderThanValidator(ChronoUnit unit, long limit) {
        this.unit = unit;
        this.limit = limit;
    }

    @Override
    public List<AttributeError> validate(CreatedEvent value) {
        if (value.start().toLocalDate().isBefore(ZonedDateTime.now(AT_ZONE_ID).toLocalDate().minus(limit, unit))) {
            return List.of(new AttributeError("dataFrom", "Date must no be older than %s %s".formatted(limit, unit)));
        }
        return List.of();
    }
}
