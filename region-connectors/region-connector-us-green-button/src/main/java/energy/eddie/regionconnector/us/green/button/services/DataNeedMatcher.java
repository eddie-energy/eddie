package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.us.green.button.client.dtos.Meter;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
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
            var dataNeed = dataNeedsService.getById(dataNeedId);
            if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
                infoOnDroppedMeter(meterUid, dataNeedId);
                continue;
            }
            var matchesAny = anyMeterBlockMatchesEnergyTypeOfDataNeed(meter, vhdDataNeed);
            if (matchesAny) {
                filteredMeters.add(meter);
            } else {
                infoOnDroppedMeter(meterUid, dataNeedId);
            }
        }
        return filteredMeters;
    }

    private static void infoOnDroppedMeter(String meterUid, String dataNeedId) {
        LOGGER.info(
                "Dropping meter {} from list of meters to collect data from since it does not support data need {}",
                meterUid,
                dataNeedId
        );
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
