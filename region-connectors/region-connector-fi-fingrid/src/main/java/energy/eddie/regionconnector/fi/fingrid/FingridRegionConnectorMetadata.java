package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class FingridRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final RegionConnectorMetadata INSTANCE = new FingridRegionConnectorMetadata();
    public static final String REGION_CONNECTOR_ID = "fi-fingrid";
    public static final ZoneId ZONE_ID_FINLAND = ZoneId.of("Europe/Helsinki");
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(ValidatedHistoricalDataDataNeed.class);

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "FI";
    }

    @Override
    public long coveredMeteringPoints() {
        return 4_000_000;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-6);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(2);
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return List.of(
                Granularity.PT15M,
                Granularity.PT1H,
                Granularity.P1D,
                Granularity.P1M,
                Granularity.P1Y
        );
    }

    @Override
    public ZoneId timeZone() {
        return ZONE_ID_FINLAND;
    }
}
