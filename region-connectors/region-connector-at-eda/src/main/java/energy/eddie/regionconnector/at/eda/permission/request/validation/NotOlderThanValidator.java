package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NotOlderThanValidator implements Validator<AtPermissionRequest> {
    private final ChronoUnit unit;

    private final long limit;

    public NotOlderThanValidator(ChronoUnit unit, long limit) {
        this.unit = unit;
        this.limit = limit;
    }

    @Override
    public List<AttributeError> validate(AtPermissionRequest value) {
        if (value.dataFrom().isBefore(LocalDate.now(Clock.systemUTC()).minus(limit, unit))) {
            return List.of(new AttributeError("dataFrom", "Date must no be older than %s %s".formatted(limit, unit)));
        }
        return List.of();
    }
}