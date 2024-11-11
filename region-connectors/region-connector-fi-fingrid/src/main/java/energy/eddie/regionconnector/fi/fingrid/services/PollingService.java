package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class PollingService implements CommonPollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnergyDataService energyDataService;
    private final FingridApiClient api;
    private final UpdateGranularityService updateGranularityService;

    public PollingService(
            EnergyDataService energyDataService, FingridApiClient api,
            UpdateGranularityService updateGranularityService
    ) {
        this.energyDataService = energyDataService;
        this.api = api;
        this.updateGranularityService = updateGranularityService;
    }

    @Override
    public void fetchMeterReadings(CommonPermissionRequest permissionRequest, LocalDate start, LocalDate end) {

    }

    public void pollTimeSeriesData(CommonPermissionRequest permissionRequest) {
        pollTimeSeriesData(permissionRequest, permissionRequest.granularity());
    }

    public void pollTimeSeriesData(CommonPermissionRequest permissionRequest, Granularity granularity) {
        var now = LocalDate.now(ZoneOffset.UTC);
        if (permissionRequest.start().isAfter(now) || permissionRequest.start().isEqual(now)) {
            return;
        }
        var start = permissionRequest.start().atStartOfDay(ZoneOffset.UTC);
        var yesterday = now.minusDays(1);
        var end = permissionRequest.end().isAfter(yesterday) ? yesterday : permissionRequest.end();
        api.getTimeSeriesData(
                   permissionRequest.meteringPointEAN(),
                   permissionRequest.customerIdentification(),
                   permissionRequest.latestMeterReading().orElse(start),
                   DateTimeUtils.endOfDay(end, ZoneOffset.UTC),
                   granularity.name(),
                   null
           )
           .flatMap(resp -> updateGranularityService.updateGranularity(resp, (FingridPermissionRequest) permissionRequest))
           .subscribe(
                   energyDataService.publish((FingridPermissionRequest) permissionRequest),
                   error -> LOGGER
                           .atInfo()
                           .addArgument(permissionRequest::permissionId)
                           .setCause(error)
                           .log("Error while requesting data for permission request {}")
           );
    }
}
