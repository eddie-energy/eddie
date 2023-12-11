package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;

public class EnerginetRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "dk-energinet";
    public static final String BASE_PATH = "/region-connectors/" + REGION_CONNECTOR_ID;
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
        return "dk";
    }

    @Override
    public long coveredMeteringPoints() {
        return 3300000;
    }
}