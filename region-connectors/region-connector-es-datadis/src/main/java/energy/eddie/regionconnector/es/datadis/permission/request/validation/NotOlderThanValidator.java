package energy.eddie.regionconnector.es.datadis.permission.request.validation;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NotOlderThanValidator implements Validator<EsPermissionRequest> {
    private final ChronoUnit unit;
    private final long limit;

    /**
     * Creates a new Validator that checks if the {@code start} time of the {@link EsPermissionRequest}
     * is before {@code now - limit}.
     * Uses {@link DatadisSpecificConstants#ZONE_ID_SPAIN} as timezone for the comparison timestamp.
     * Assumes non-null values.
     *
     * @param limit Duration to subtract from the current time resulting in the earliest date for which data may be requested.
     */
    public NotOlderThanValidator(ChronoUnit unit, long limit) {
        this.unit = unit;
        this.limit = limit;
    }

    @Override
    public List<AttributeError> validate(EsPermissionRequest value) {
        var earliestAllowedStart = ZonedDateTime.now(DatadisSpecificConstants.ZONE_ID_SPAIN).minus(limit, unit);
        if (value.requestDataFrom().isBefore(earliestAllowedStart)) {
            return List.of(new AttributeError("requestDataFrom", "requestDataFrom must not be older than %s %s".formatted(limit, unit)));
        }
        return List.of();
    }
}
