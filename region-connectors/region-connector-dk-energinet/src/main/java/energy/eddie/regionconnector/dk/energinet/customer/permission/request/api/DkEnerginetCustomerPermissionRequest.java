package energy.eddie.regionconnector.dk.energinet.customer.permission.request.api;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    String refreshToken();

    PeriodResolutionEnum periodResolution();

    String meteringPoint();
}
