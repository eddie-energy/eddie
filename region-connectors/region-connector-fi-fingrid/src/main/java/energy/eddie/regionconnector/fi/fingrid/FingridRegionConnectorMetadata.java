package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;

import java.time.Period;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public class FingridRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final RegionConnectorMetadata INSTANCE = new FingridRegionConnectorMetadata();
    public static final String REGION_CONNECTOR_ID = "fi-fingrid";
    public static final ZoneId ZONE_ID_FINLAND = ZoneId.of("Europe/Helsinki");

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
        return 0;
    }

    @Override
    public Period earliestStart() {
        // TODO: GH-1152
        return Period.ofYears(-3);
    }

    @Override
    public Period latestEnd() {
        // TODO: GH-1152
        return Period.ofYears(3);
    }

    @Override
    public List<Granularity> supportedGranularities() {
        // TODO: GH-1152
        return Arrays.asList(Granularity.values());
    }

    @Override
    public ZoneId timeZone() {
        return ZONE_ID_FINLAND;
    }
}
