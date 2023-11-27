package energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NotOlderThanValidator implements Validator<DkEnerginetCustomerPermissionRequest> {
    private static final ZoneId zoneId = EnerginetRegionConnector.DK_ZONE_ID;
    private final ChronoUnit unit;
    private final long limit;

    /**
     * Creates a new Validator that checks if the {@code start} time of the {@link DkEnerginetCustomerPermissionRequest}
     * is before {@code now - limit}.
     * Uses {@link EnerginetRegionConnector#DK_ZONE_ID} as timezone for the comparison timestamp.
     * Assumes non-null values.
     *
     * @param limit Duration to subtract from the current time resulting in the earliest date for which data may be requested.
     */
    public NotOlderThanValidator(ChronoUnit unit, long limit) {
        this.unit = unit;
        this.limit = limit;
    }

    @Override
    public List<AttributeError> validate(DkEnerginetCustomerPermissionRequest value) {
        var earliestAllowedStart = ZonedDateTime.now(zoneId).minus(limit, unit);
        if (value.start().isBefore(earliestAllowedStart)) {
            return List.of(new AttributeError("start", "start must not be older than %s %s".formatted(limit, unit)));
        }
        return List.of();
    }
}
