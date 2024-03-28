package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;

import java.util.Collections;
import java.util.List;


/**
 * A Validator that checks if the {@code start} time of the {@link PermissionRequest} is before or equal to the
 * {@code end} time. Assumes non-null values.
 */
public class StartIsBeforeOrEqualEndValidator<T extends PermissionRequest> implements Validator<T> {
    @Override
    public List<AttributeError> validate(T value) {
        if (value.start().isAfter(value.end())) {
            return List.of(new AttributeError("start", "start must be before or equal to end"));
        }
        return Collections.emptyList();
    }
}
