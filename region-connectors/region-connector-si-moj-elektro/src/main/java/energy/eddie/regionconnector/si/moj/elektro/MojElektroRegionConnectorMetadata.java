package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
public class MojElektroRegionConnectorMetadata implements RegionConnectorMetadata {

    public static final String REGION_CONNECTOR_ID = "si-moj-elektro";
    public static final String COUNTRY_CODE = "SI";

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
        return Period.ZERO;
    }

    @Override
    public Period latestEnd() {
        return Period.ZERO;
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }
}
