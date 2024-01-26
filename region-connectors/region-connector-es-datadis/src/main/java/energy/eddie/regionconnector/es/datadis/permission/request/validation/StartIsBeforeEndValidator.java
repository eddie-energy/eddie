package energy.eddie.regionconnector.es.datadis.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.util.Collections;
import java.util.List;


/**
 * A Validator that checks if the {@code start} time of the {@link EsPermissionRequest}
 * is before the {@code end} time.
 * Assumes non-null values.
 */
public class StartIsBeforeEndValidator implements Validator<EsPermissionRequest> {
    @Override
    public List<AttributeError> validate(EsPermissionRequest value) {

        if (!value.start().isAfter(value.end())) {
            return Collections.emptyList();
        }

        return List.of(new AttributeError("requestDataFrom", "requestDataFrom must be before requestDataTo"));
    }
}