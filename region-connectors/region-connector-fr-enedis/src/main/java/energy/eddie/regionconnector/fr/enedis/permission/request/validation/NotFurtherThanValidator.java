package energy.eddie.regionconnector.fr.enedis.permission.request.validation;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A validator that checks if the end date of the PermissionRequest is not further in the future than the limit allows.
 */
public class NotFurtherThanValidator implements Validator<TimeframedPermissionRequest> {
    private final ChronoUnit unit;

    private final long limit;

    public NotFurtherThanValidator(ChronoUnit unit, long limit) {
        this.unit = unit;
        this.limit = limit;
    }

    @Override
    public List<AttributeError> validate(TimeframedPermissionRequest value) {
        ZonedDateTime limitDate = ZonedDateTime
                .now(ZoneOffset.UTC)
                .plus(limit, unit);
        if (value.end().isAfter(limitDate)) {
            return List.of(new AttributeError("end", "Date must not be further in the future than %s %s".formatted(limit, unit)));
        }
        return List.of();
    }
}