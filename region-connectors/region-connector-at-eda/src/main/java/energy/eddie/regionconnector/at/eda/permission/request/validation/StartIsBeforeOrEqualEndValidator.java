package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

import java.time.ZonedDateTime;
import java.util.List;

public class StartIsBeforeOrEqualEndValidator implements Validator<AtPermissionRequest> {
    @Override
    public List<AttributeError> validate(AtPermissionRequest value) {
        ZonedDateTime dataTo = value.end();
        if (dataTo == null || !dataTo.isBefore(value.start())) {
            return List.of();
        }
        return List.of(new AttributeError("dataFrom", "DateFrom must be before or equal to DateTo"));
    }
}