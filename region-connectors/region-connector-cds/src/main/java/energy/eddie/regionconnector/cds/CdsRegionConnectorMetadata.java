package energy.eddie.regionconnector.cds;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Component
public class CdsRegionConnectorMetadata implements RegionConnectorMetadata {

    public static final String REGION_CONNECTOR_ID = "cds";

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public List<String> countryCodes() {
        return List.of("US", "CA");
    }

    @Override
    public String countryCode() {
        return "US/CA";
    }

    @Override
    public long coveredMeteringPoints() {
        return 0;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-2);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(3);
    }

    @Override
    public List<Granularity> supportedGranularities() {
        return Arrays.asList(Granularity.values());
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        return Arrays.asList(EnergyType.values());
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.of(ValidatedHistoricalDataDataNeed.class);
    }
}
