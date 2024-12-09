package energy.eddie.regionconnector.us.green.button.services.historical.collection;

import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataNeedMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedMatcher.class);
    private final DataNeedsService dataNeedsService;
    private final UsPermissionRequestRepository repository;

    public DataNeedMatcher(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            UsPermissionRequestRepository repository
    ) {
        this.dataNeedsService = dataNeedsService;
        this.repository = repository;
    }

    public List<Meter> filterMetersNotMeetingDataNeedCriteria(MeterListing response) {
        var filteredMeters = new ArrayList<Meter>();
        for (var meter : response.meters()) {
            var authUid = meter.authorizationUid();
            var meterUid = meter.uid();
            var permissionRequest = repository.findByAuthUid(authUid);
            var dataNeedId = permissionRequest.dataNeedId();
            if (isRelevantEnergyType(meter, permissionRequest)) {
                filteredMeters.add(meter);
                continue;
            }
            LOGGER.info(
                    "Dropping meter {} from list of meters to collect data from since it does not support data need {}",
                    meterUid,
                    dataNeedId
            );
        }
        return filteredMeters;
    }

    public boolean isRelevantEnergyType(Meter meter, UsGreenButtonPermissionRequest permissionRequest) {
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            return false;
        }
        return anyMeterBlockMatchesEnergyTypeOfDataNeed(meter, vhdDataNeed);
    }

    private static boolean anyMeterBlockMatchesEnergyTypeOfDataNeed(
            Meter meter,
            ValidatedHistoricalDataDataNeed vhdDataNeed
    ) {
        var matchesAny = false;
        for (var block : meter.meterBlocks().values()) {
            matchesAny = matchesAny || matchesEnergyType(block.serviceClass(), vhdDataNeed.energyType());
        }
        return matchesAny;
    }

    private static boolean matchesEnergyType(String serviceClass, EnergyType energyType) {
        var postfix = switch (energyType) {
            case ELECTRICITY -> "electric";
            case NATURAL_GAS -> "gas";
            case HYDROGEN -> "water";
            case HEAT -> "heat";
        };
        return serviceClass.endsWith(postfix);
    }
}
