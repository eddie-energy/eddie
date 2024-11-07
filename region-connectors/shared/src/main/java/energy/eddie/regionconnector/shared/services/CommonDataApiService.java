package energy.eddie.regionconnector.shared.services;

import java.time.LocalDate;

public interface CommonDataApiService {
    void fetchDataForPermissionRequest(CommonPermissionRequest permissionRequest, LocalDate start, LocalDate end);
}
