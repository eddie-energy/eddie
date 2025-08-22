package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class SiMojElektroRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "si-moj-elektro";
    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "SI";
    }

    @Override
    public long coveredMeteringPoints() {
        return 0;
    }

    @Override
    public Period earliestStart() {
        return Period.ZERO;
    }

    @Override
    public Period latestEnd() {
        return Period.ZERO;
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return List.of();
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        return List.of();
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.of();
    }
}
