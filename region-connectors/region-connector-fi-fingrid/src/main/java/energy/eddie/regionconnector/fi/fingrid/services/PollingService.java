package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class PollingService implements CommonPollingService<FingridPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnergyDataService energyDataService;
    private final FingridApiClient api;
    private final UpdateGranularityService updateGranularityService;
    private final DataNeedsService dataNeedsService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    // DataNeedsService is injected from parent context
    public PollingService(
            EnergyDataService energyDataService, FingridApiClient api,
            UpdateGranularityService updateGranularityService,
            DataNeedsService dataNeedsService
    ) {
        this.energyDataService = energyDataService;
        this.api = api;
        this.updateGranularityService = updateGranularityService;
        this.dataNeedsService = dataNeedsService;
    }

    @Override
    public void pollTimeSeriesData(FingridPermissionRequest permissionRequest) {
        pollTimeSeriesData(permissionRequest, permissionRequest.granularity());
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(FingridPermissionRequest permissionRequest) {
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);
        return dataNeed instanceof ValidatedHistoricalDataDataNeed;
    }

    public void pollTimeSeriesData(FingridPermissionRequest permissionRequest, Granularity granularity) {
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
                .flatMap(resp -> updateGranularityService.updateGranularity(resp, permissionRequest))
                .subscribe(
                        energyDataService.publish(permissionRequest),
                        error -> LOGGER
                                .atInfo()
                                .addArgument(permissionRequest::permissionId)
                                .setCause(error)
                                .log("Error while requesting data for permission request {}")
                );
    }
}
