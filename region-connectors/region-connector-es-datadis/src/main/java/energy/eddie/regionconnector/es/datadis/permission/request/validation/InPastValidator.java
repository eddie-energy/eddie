package energy.eddie.regionconnector.es.datadis.permission.request.validation;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;


/**
 * A Validator that checks if the {@code start} and {@code end} time of the {@link EsPermissionRequest}
 * are both in the past, i.e. before or equal to now.minusDays(1).
 * Uses {@link DatadisSpecificConstants#ZONE_ID_SPAIN} as timezone for the comparison timestamp.
 * Assumes non-null values.
 */
public class InPastValidator implements Validator<EsPermissionRequest> {
    @Override
    public List<AttributeError> validate(EsPermissionRequest value) {
        var nowMinus1Day = ZonedDateTime.now(ZONE_ID_SPAIN).minusDays(1);

        if (nowMinus1Day.isBefore(value.requestDataFrom()) || nowMinus1Day.isBefore(value.requestDataTo())) {
            return List.of(new AttributeError("requestDataFrom", "requestDataFrom and requestDataTo must be completely in the past (i.e. at least 1 day in the past)"));
        }

        return Collections.emptyList();
    }
}
