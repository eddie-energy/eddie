package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class EnerginetRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "dk-energinet";
    public static final ZoneId DK_ZONE_ID = ZoneId.of("Europe/Copenhagen");
    public static final Period PERIOD_EARLIEST_START = Period.ofYears(-4);
    // Currently we only support the customer api and the token for this is valid for a maximum of 1 year
    public static final Period PERIOD_LATEST_END = Period.ofYears(1);
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.PT15M,
                                                                            Granularity.PT1H,
                                                                            Granularity.P1D,
                                                                            Granularity.P1M,
                                                                            Granularity.P1Y);
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(
            ValidatedHistoricalDataDataNeed.class,
            AccountingPointDataNeed.class
    );

    @Nullable
    private static EnerginetRegionConnectorMetadata instance = null;

    private EnerginetRegionConnectorMetadata() {
    }

    public static EnerginetRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new EnerginetRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "DK";
    }

    @Override
    public long coveredMeteringPoints() {
        return 3300000;
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
        return DK_ZONE_ID;
    }
}
