package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.time.LocalDate;

public interface CommonPollingService {

//
//    default void fetchMeterReadings(
//            MeterReadingPermissionRequest permissionRequest,
//            LocalDate start,
//            LocalDate end
//    ){
//
//    }

    void pollTimeSeriesData(CommonPermissionRequest activePermission);
}
