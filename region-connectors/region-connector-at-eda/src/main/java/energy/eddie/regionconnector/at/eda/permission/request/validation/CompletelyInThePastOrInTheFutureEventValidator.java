package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@SuppressWarnings("DuplicatedCode")
public class CompletelyInThePastOrInTheFutureEventValidator implements Validator<CreatedEvent> {
    @Override
    public List<AttributeError> validate(CreatedEvent value) {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        boolean isEndInThePast = Optional.ofNullable(value.end()).map(dataTo -> dataTo.isBefore(now)).orElse(false);
        boolean isCompletelyInThePast = value.start().isBefore(now) && isEndInThePast;
        if (isCompletelyInThePast
                || isFuture(value, now)
                || isPresent(value, now)) {
            return List.of();
        }
        return List.of(new AttributeError("start",
                                          "start and end must lie completely in the past or completely in the future"));
    }

    private boolean isFuture(CreatedEvent value, LocalDate now) {
        return value.start().isAfter(now);
    }

    private boolean isPresent(CreatedEvent value, LocalDate now) {
        return value.start().isEqual(now);
    }
}
