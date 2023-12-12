package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;

public class DatadisRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "es-datadis";
    public static final String BASE_PATH = "/region-connectors/" + REGION_CONNECTOR_ID;
    @Nullable
    private static DatadisRegionConnectorMetadata instance = null;

    private DatadisRegionConnectorMetadata() {
    }

    public static DatadisRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new DatadisRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "ES";
    }

    @Override
    public long coveredMeteringPoints() {
        return 30234170;
    }
}