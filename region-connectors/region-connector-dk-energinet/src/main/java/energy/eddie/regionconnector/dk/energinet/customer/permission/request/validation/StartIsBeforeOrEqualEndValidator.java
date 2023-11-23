package energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

import java.util.Collections;
import java.util.List;


/**
 * A Validator that checks if the {@code start} time of the {@link DkEnerginetCustomerPermissionRequest}
 * is before or equal to the {@code end} time.
 * Assumes non-null values.
 */
public class StartIsBeforeOrEqualEndValidator implements Validator<DkEnerginetCustomerPermissionRequest> {
    @Override
    public List<AttributeError> validate(DkEnerginetCustomerPermissionRequest value) {
        if (value.start().isAfter(value.end())) {
            return List.of(new AttributeError("start", "start must be after end"));
        }

        return Collections.emptyList();
    }
}
