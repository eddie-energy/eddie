package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;

import java.time.ZonedDateTime;

public record SimplePermissionRequest(String permissionId, String connectionId, ZonedDateTime start, ZonedDateTime end,
                                      PermissionRequestState state) implements DkEnerginetCustomerPermissionRequest {
    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, null, null);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, ZonedDateTime start, ZonedDateTime end) {
        this(permissionId, connectionId, start, end, null);
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public void changeState(PermissionRequestState state) {

    }

    @Override
    public String refreshToken() {
        return "refreshToken";
    }

    @Override
    public TimeSeriesAggregationEnum aggregation() {
        return TimeSeriesAggregationEnum.ACTUAL;
    }

    @Override
    public String meteringPoint() {
        return "meteringPoint";
    }
}
