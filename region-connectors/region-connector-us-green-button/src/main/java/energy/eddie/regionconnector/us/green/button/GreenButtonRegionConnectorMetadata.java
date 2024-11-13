package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import jakarta.annotation.Nullable;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class GreenButtonRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "us-green-button";
    public static final Period PERIOD_EARLIEST_START = Period.ofMonths(-24);
    public static final Period PERIOD_LATEST_END = Period.ofMonths(36);
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.PT15M, Granularity.P1D);
    public static final ZoneId US_ZONE_ID = ZoneId.of("America/New_York");
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(ValidatedHistoricalDataDataNeed.class);

    @Nullable
    private static GreenButtonRegionConnectorMetadata instance = null;

    private GreenButtonRegionConnectorMetadata() {}

    public static GreenButtonRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new GreenButtonRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public List<String> countryCodes() {
        return List.of("US", "CA");
    }

    @Override
    public String countryCode() {
        return "US/CA";
    }

    @Override
    public long coveredMeteringPoints() {
        return 47000000;
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
        return US_ZONE_ID;
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.copyOf(SUPPORTED_DATA_NEEDS);
    }
}
