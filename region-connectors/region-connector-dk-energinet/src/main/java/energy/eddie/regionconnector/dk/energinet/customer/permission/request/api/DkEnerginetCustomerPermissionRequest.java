package energy.eddie.regionconnector.dk.energinet.customer.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    String refreshToken();

    Granularity granularity();

    String meteringPoint();
}