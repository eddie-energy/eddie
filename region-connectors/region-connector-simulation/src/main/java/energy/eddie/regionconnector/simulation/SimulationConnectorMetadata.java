package energy.eddie.regionconnector.simulation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.utils.DataNeedUtils;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class SimulationConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "sim";
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = new ArrayList<>();


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
        return "DE";
    }

    @Override
    public long coveredMeteringPoints() {
        return 1;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-1000);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(9999);
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return List.of(Granularity.values());
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public List<String> supportedDataNeeds() {
        return DataNeedUtils.convertDataNeedClassesToString(SUPPORTED_DATA_NEEDS);
    }
}