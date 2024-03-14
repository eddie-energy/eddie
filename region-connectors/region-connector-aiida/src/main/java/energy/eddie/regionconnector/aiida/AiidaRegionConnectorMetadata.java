package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.ZoneId;

public class AiidaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "aiida";
    public static final ZoneId REGION_CONNECTOR_ZONE_ID = ZoneId.of("Etc/UTC");

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
