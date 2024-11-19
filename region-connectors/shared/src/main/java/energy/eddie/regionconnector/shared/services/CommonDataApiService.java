package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

public interface CommonDataApiService<T extends PermissionRequest> {
    void pollTimeSeriesData(T permissionRequest, String timeZone);
}
