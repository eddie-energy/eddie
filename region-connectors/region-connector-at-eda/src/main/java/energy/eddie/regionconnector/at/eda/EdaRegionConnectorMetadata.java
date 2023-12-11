package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;

public class EdaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "at-eda";

    public static final String BASE_PATH = "/region-connectors/" + REGION_CONNECTOR_ID;
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
        return "at";
    }

    @Override
    public long coveredMeteringPoints() {
        return 5977915;
    }
}