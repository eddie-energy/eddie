package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

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
     * Supported granularities for metered data in Germany
     * PT15M = 15-minute intervals
     * PT1H = Hourly intervals
     * P1D = Daily intervals
     */
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(
        Granularity.PT15M, 
        Granularity.PT1H, 
        Granularity.P1D
    );
    
    /**
     * Supported data need types
     */
    public static final List<Class<? extends DataNeedInterface>> SUPPORTED_DATA_NEEDS = List.of(
        ValidatedHistoricalDataDataNeed.class,
        AccountingPointDataNeed.class
    );

    /**
     * Approximate number of metering points covered in Germany
     * This is an estimate and should be updated with actual data from ETA Plus
     */
    private static final long COVERED_METERING_POINTS = 50000000L; // ~50 million metering points in Germany
    
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
        return "DE";
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
    public List<Granularity> supportedGranularities() {
        return SUPPORTED_GRANULARITIES;
    }

    @Override
    public ZoneId timeZone() {
        return DE_ZONE_ID;
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        return List.of(EnergyType.ELECTRICITY, EnergyType.NATURAL_GAS);
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.copyOf(SUPPORTED_DATA_NEEDS);
    }
}
