package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class CompletelyInThePastOrInTheFutureValidator<T extends PermissionRequest> implements Validator<T> {

    @Override
    public List<AttributeError> validate(T value) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        boolean isEndInThePast = Optional.ofNullable(value.end()).map(dataTo -> dataTo.isBefore(now)).orElse(false);
        boolean completelyInThePast = value.start().isBefore(now) && isEndInThePast;
        if (completelyInThePast
                || isFuture(value, now)
                || isPresentToFuture(value, now)) {
            return List.of();
        }
        return List.of(new AttributeError("start",
                                          "start and end must lie completely in the past or completely in the future"));
    }

    private boolean isFuture(T value, LocalDate now) {
        return value.start().isAfter(now);
    }

    private boolean isPresentToFuture(T value, LocalDate now) {
        return value.start().isEqual(now);
    }
}
