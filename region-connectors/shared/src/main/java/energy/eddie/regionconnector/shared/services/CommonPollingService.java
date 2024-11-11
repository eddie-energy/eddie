package energy.eddie.regionconnector.shared.services;

import java.time.LocalDate;

public interface CommonPollingService {


    void fetchMeterReadings(
            CommonPermissionRequest permissionRequest,
            LocalDate start,
            LocalDate end
    );

    void pollTimeSeriesData(CommonPermissionRequest activePermission);
}
