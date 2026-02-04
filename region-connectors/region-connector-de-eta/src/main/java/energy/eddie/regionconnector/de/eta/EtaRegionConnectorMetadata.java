package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.v0.RegionConnectorMetadata;

import jakarta.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;

/**
 * Metadata for the ETA Plus (Germany) region connector.
 * This class provides essential information about the region connector's capabilities,
 * supported features, and regional specifics.
 */
public class EtaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "de-eta";
    public static final String COUNTRY_CODE = "DE";
    
    /**
     * Historical data can be requested up to 36 months in the past
     */
    public static final Period PERIOD_EARLIEST_START = Period.ofMonths(-36);
    
    /**
     * Permissions can be granted up to 36 months in the future
     */
    public static final Period PERIOD_LATEST_END = Period.ofMonths(36);
    
    /**
     * Germany uses Central European Time
     */
    public static final ZoneId DE_ZONE_ID = ZoneId.of("Europe/Berlin");
    
    /**
     * Approximate number of metering points covered in Germany
     */
    private static final long COVERED_METERING_POINTS = 500000;
    
    @Nullable
    private static EtaRegionConnectorMetadata instance = null;

    private EtaRegionConnectorMetadata() {
        // Private constructor for singleton pattern
    }

    /**
     * Get the singleton instance of the metadata
     * @return the metadata instance
     */
    public static EtaRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new EtaRegionConnectorMetadata();
        }
        return instance;
    }

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
        return COVERED_METERING_POINTS;
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
        return DE_ZONE_ID;
    }

}
