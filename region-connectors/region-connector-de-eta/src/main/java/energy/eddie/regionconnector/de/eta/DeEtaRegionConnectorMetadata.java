package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;

@Component
public class DeEtaRegionConnectorMetadata implements RegionConnectorMetadata {

    public static final String REGION_CONNECTOR_ID = "de-eta";
    public static final String COUNTRY_CODE = "DE";

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return COUNTRY_CODE;
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
        return List.of(Granularity.values());
    }

    @Override
    public ZoneId timeZone() {
        return ZoneId.of("Europe/Berlin");
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
