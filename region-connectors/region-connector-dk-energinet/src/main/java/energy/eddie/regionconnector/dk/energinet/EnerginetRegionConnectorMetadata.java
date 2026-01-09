package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;

public class EnerginetRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "dk-energinet";
    public static final ZoneId DK_ZONE_ID = ZoneId.of("Europe/Copenhagen");
    public static final Period PERIOD_EARLIEST_START = Period.ofYears(-4);
    // Currently we only support the customer api and the token for this is valid for a maximum of 1 year
    public static final Period PERIOD_LATEST_END = Period.ofYears(1);
    /**
     * The Global Location Number (GLN) of the sender of the message. This value is from the <a
     * href="https://api.eloverblik.dk/customerapi/index.html">API documentation</a> (from the API description linked in
     * the description)
     */
    public static final String GLOBAL_LOCATION_NUMBER = "5790001330583";

    @Nullable
    private static EnerginetRegionConnectorMetadata instance = null;

    private EnerginetRegionConnectorMetadata() {
    }

    public static EnerginetRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new EnerginetRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "DK";
    }

    @Override
    public long coveredMeteringPoints() {
        return 3300000;
    }

    @Override
    public Period earliestStart() {
        return PERIOD_EARLIEST_START;
    }

    @Override
    public Period latestEnd() {
        return PERIOD_LATEST_END;
    }

    @Override
    public ZoneId timeZone() {
        return DK_ZONE_ID;
    }
}
