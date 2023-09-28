package energy.eddie.regionconnector.dk.energinet.customer.api;

import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    String refreshToken();

    TimeSeriesAggregationEnum aggregation();

    String meteringPoint();
}
