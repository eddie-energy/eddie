package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class EdaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "at-eda";
    public static final AllowedTransmissionCycle TRANSMISSION_CYCLE = AllowedTransmissionCycle.D;
    public static final Period PERIOD_EARLIEST_START = Period.ofMonths(-36);
    public static final Period PERIOD_LATEST_END = Period.ofMonths(36);
    public static final ZoneId AT_ZONE_ID = ZoneId.of("Europe/Vienna");
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.PT15M, Granularity.P1D);
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(ValidatedHistoricalDataDataNeed.class,
                                                                                       AccountingPointDataNeed.class);
    @Nullable
    private static EdaRegionConnectorMetadata instance = null;

    private EdaRegionConnectorMetadata() {
    }

    public static EdaRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new EdaRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "AT";
    }

    @Override
    public long coveredMeteringPoints() {
        return 5977915;
    }

    @Override
    public Period earliestStart() {
        return PERIOD_EARLIEST_START;
    }

    @Override
    public Period latestEnd() {
        return PERIOD_LATEST_END;
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return SUPPORTED_GRANULARITIES;
    }

    @Override
    public ZoneId timeZone() {
        return AT_ZONE_ID;
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        return List.of(EnergyType.ELECTRICITY);
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.copyOf(SUPPORTED_DATA_NEEDS);
    }
}
