package energy.eddie.regionconnector.nl.mijn.aansluiting;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.EnergyType;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.SequencedCollection;

public class MijnAansluitingRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "nl-mijn-aansluiting";
    public static final Period MAX_PERIOD_IN_PAST = Period.ofYears(2);
    public static final Period MAX_PERIOD_IN_FUTURE = Period.ofYears(Integer.MAX_VALUE);
    public static final SequencedCollection<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.P1D);
    public static final SequencedCollection<EnergyType> SUPPORTED_ENERGY_TYPES = List.of(EnergyType.NATURAL_GAS,
                                                                                         EnergyType.ELECTRICITY);
    public static final ZoneId NL_ZONE_ID = ZoneId.of("Europe/Amsterdam");
    @Nullable
    private static MijnAansluitingRegionConnectorMetadata instance = null;

    private MijnAansluitingRegionConnectorMetadata() {
    }

    public static MijnAansluitingRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new MijnAansluitingRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "NL";
    }

    @Override
    public long coveredMeteringPoints() {
        // TODO calculate/find out, see GH-946
        return 0;
    }
}
