package energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;


/**
 * A Validator that checks if the {@code start} and {@code end} time of the {@link DkEnerginetCustomerPermissionRequest}
 * are both in the past.
 * Uses <i>Europe/Copenhagen</i> as timezone for the comparison timestamp.
 * Assumes non-null values.
 */
public class CompletelyInThePastValidator implements Validator<DkEnerginetCustomerPermissionRequest> {
    @Override
    public List<AttributeError> validate(DkEnerginetCustomerPermissionRequest value) {
        var now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));

        if (value.end().isAfter(now) || value.start().isAfter(now)) {
            return List.of(new AttributeError("end", "start and end must be completely in the past"));
        }

        return Collections.emptyList();
    }
}
