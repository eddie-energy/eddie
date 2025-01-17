package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class EnedisRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "fr-enedis";
    public static final Period PERIOD_EARLIEST_START = Period.ofYears(-3);
    public static final Period PERIOD_LATEST_END = Period.ofYears(3);
    public static final ZoneId ZONE_ID_FR = ZoneId.of("Europe/Paris");
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(ValidatedHistoricalDataDataNeed.class,
                                                                                       AccountingPointDataNeed.class);
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.PT30M, Granularity.P1D);

    @Nullable
    private static EnedisRegionConnectorMetadata instance = null;

    private EnedisRegionConnectorMetadata() {
    }

    public static EnedisRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new EnedisRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "FR";
    }

    @Override
    public long coveredMeteringPoints() {
        return 36951446;
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
        return ZONE_ID_FR;
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
