package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class CompletelyInThePastOrInTheFutureEventValidator implements Validator<CreatedEvent> {
    private boolean isPresent(CreatedEvent value, ZonedDateTime now) {
        return value.start().isEqual(now);
    }

    private boolean isFuture(CreatedEvent value, ZonedDateTime now) {
        return value.start().isAfter(now);
    }

    @Override
    public List<AttributeError> validate(CreatedEvent value) {
        ZonedDateTime now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
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
}