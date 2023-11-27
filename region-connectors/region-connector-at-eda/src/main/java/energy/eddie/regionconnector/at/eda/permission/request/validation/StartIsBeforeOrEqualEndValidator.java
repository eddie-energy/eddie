package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class StartIsBeforeOrEqualEndValidator implements Validator<AtPermissionRequest> {
    @Override
    public List<AttributeError> validate(AtPermissionRequest value) {
        Optional<LocalDate> dataTo = value.dataTo();
        if (dataTo.isEmpty() || !dataTo.get().isBefore(value.dataFrom())) {
            return List.of();
        }
        return List.of(new AttributeError("dataFrom", "DateFrom must be before or equal to DateTo"));
    }
}