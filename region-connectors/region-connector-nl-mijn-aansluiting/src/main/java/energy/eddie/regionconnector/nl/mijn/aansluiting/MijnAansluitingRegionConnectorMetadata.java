package energy.eddie.regionconnector.nl.mijn.aansluiting;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;

public class MijnAansluitingRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "nl-mijn-aansluiting";
    public static final Period MAX_PERIOD_IN_PAST = Period.ofYears(-2);
    public static final Period MAX_PERIOD_IN_FUTURE = Period.ofYears(9999);
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
        // Source: https://github.com/eddie-energy/eddie/issues/487#issue-2019640488
        // Alliander | 3 700 000
        // Enexis Netbeheer B.V. | 2 849 000
        // Stedin Groep | 2 000 000
        return 3_700_000L + 2_849_000L + 2_000_000L;
    }

    @Override
    public Period earliestStart() {
        return MAX_PERIOD_IN_PAST;
    }

    @Override
    public Period latestEnd() {
        return MAX_PERIOD_IN_FUTURE;
    }

    @Override
    public ZoneId timeZone() {
        return NL_ZONE_ID;
    }
}
