package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;

public class EnedisRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "fr-enedis";

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