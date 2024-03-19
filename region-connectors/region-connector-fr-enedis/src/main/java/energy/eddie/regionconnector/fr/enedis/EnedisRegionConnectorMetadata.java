package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.Period;

public class EnedisRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "fr-enedis";
    public static final Period PERIOD_EARLIEST_START = Period.ofYears(-3);
    public static final Period PERIOD_LATEST_END = Period.ofYears(3);

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
}
