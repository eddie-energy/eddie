package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;

@SuppressWarnings("java:S6548") // False positive
@Component
public class FluviusRegionConnectorMetadata implements RegionConnectorMetadata {

    public static final String REGION_CONNECTOR_ID = "be-fluvius";
    public static final ZoneId ZONE_ID_BELGIUM = ZoneId.of("Europe/Brussels");

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "BE";
    }

    /**
     * According to this <a href="https://over.fluvius.be/sites/fluvius/files/2021-06/fluvius-investor-presentation-june-2021.pdf">presentation</a> Fluvius services 5.8 electricity and gas metering points.
     *
     * @return number of metering points covered by Fluvius
     */
    @Override
    public long coveredMeteringPoints() {
        return 5_800_000;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-3);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(3);
    }

    @Override
    public ZoneId timeZone() {
        return ZONE_ID_BELGIUM;
    }
}
