package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;

public class SimulationConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "sim";

    @Nullable
    private static SimulationConnectorMetadata instance = null;

    private SimulationConnectorMetadata() {
    }

    public static SimulationConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new SimulationConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "de";
    }

    @Override
    public long coveredMeteringPoints() {
        return 1;
    }
}