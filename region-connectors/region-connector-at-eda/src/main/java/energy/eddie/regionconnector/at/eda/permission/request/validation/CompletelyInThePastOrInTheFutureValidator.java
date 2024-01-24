package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.utils.DateTimeConstants.AT_ZONE_ID;

public class CompletelyInThePastOrInTheFutureValidator implements Validator<AtPermissionRequest> {

    private static boolean isPresentToFuture(AtPermissionRequest value, LocalDate now) {
        return value.start().toLocalDate().isEqual(now);
    }

    private static boolean isFuture(AtPermissionRequest value, LocalDate now) {
        return value.start().toLocalDate().isAfter(now);
    }

    @Override
    public List<AttributeError> validate(AtPermissionRequest value) {
        LocalDate now = ZonedDateTime.now(AT_ZONE_ID).toLocalDate();
        boolean isDataToInThePast = Optional.ofNullable(value.end()).map(dataTo -> dataTo.toLocalDate().isBefore(now)).orElse(false);
        boolean completelyInThePast = value.start().toLocalDate().isBefore(now) && isDataToInThePast;
        if (completelyInThePast
                || isFuture(value, now)
                || isPresentToFuture(value, now)) {
            return List.of();
        }
        return List.of(new AttributeError("dataFrom", "dataFrom and dataTo must lie completely in the past or completely in the future"));
    }
}