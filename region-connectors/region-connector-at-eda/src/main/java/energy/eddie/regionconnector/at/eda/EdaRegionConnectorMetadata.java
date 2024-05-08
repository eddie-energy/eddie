package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
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
}
