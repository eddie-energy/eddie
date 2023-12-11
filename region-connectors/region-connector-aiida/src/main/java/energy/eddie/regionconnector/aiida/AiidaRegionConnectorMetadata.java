package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;

public class AiidaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "aiida";
    public static final String BASE_PATH = "/region-connectors/" + REGION_CONNECTOR_ID;

    @Nullable
    private static AiidaRegionConnectorMetadata instance = null;

    private AiidaRegionConnectorMetadata() {
    }

    public static AiidaRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new AiidaRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public long coveredMeteringPoints() {
        return 1;
    }
}
