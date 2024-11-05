package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.regionconnector.shared.utils.DataNeedUtils;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

public class AiidaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "aiida";
    public static final ZoneId REGION_CONNECTOR_ZONE_ID = ZoneId.of("Etc/UTC");
    public static final String MQTT_CLIENT_ID = "eddie-region-connector-aiida";
    public static final Period EARLIEST_START = Period.ZERO;
    public static final Period LATEST_END = Period.ofYears(9999);
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(
            GenericAiidaDataNeed.class
    );

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

    @Override
    public Period earliestStart() {
        return EARLIEST_START;
    }

    @Override
    public Period latestEnd() {
        return LATEST_END;
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return List.of();
    }

    @Override
    public ZoneId timeZone() {
        return REGION_CONNECTOR_ZONE_ID;
    }

    @Override
    public List<String> supportedDataNeeds() {
        return DataNeedUtils.convertDataNeedClassesToString(SUPPORTED_DATA_NEEDS);
    }
}
