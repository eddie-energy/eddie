package energy.eddie.regionconnector.dk.energinet.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;


/**
 * A Validator that checks if the {@code start} and {@code end} time of the {@link DkEnerginetCustomerPermissionRequest}
 * are both not after now, i.e. they are either in the past or equal to now.
 * Uses {@link EnerginetRegionConnector#DK_ZONE_ID} as timezone for the comparison timestamp.
 * Assumes non-null values.
 */
public class NotAfterNowValidator implements Validator<DkEnerginetCustomerPermissionRequest> {
    @Override
    public List<AttributeError> validate(DkEnerginetCustomerPermissionRequest value) {
        var now = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID);

        if (value.end().isAfter(now) || value.start().isAfter(now)) {
            return List.of(new AttributeError("end", "start and end must be completely in the past"));
        }

        return Collections.emptyList();
    }
}