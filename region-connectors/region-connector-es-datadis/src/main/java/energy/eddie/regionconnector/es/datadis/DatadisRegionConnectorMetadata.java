package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static energy.eddie.api.agnostic.Granularity.PT1H;

public class DatadisRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "es-datadis";
    public static final ZoneId ZONE_ID_SPAIN = ZoneId.of("Europe/Madrid");
    /**
     * Datadis gives access to metering data for a maximum of 24 months in the past.
     */
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 24;
    /**
     * Datadis can grant permissions for a maximum of 24 months in the future. If no end date is provided, the end date
     * will be set to the current date plus 24 months.
     */
    public static final int MAXIMUM_MONTHS_IN_THE_FUTURE = 24;
    /**
     * The maximum time in the future that a permission request can be created for. 24 months minus one day. Datadis API
     * is exclusive on the end date, so we need to subtract one day here.
     */
    public static final Period PERIOD_LATEST_END = Period.ofMonths(MAXIMUM_MONTHS_IN_THE_FUTURE).minusDays(1);
    public static final Period PERIOD_EARLIEST_START = Period.ofMonths(-MAXIMUM_MONTHS_IN_THE_PAST);
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(PT15M, PT1H);
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(
            ValidatedHistoricalDataDataNeed.class,
            AccountingPointDataNeed.class
    );
    @Nullable
    private static DatadisRegionConnectorMetadata instance = null;

    private DatadisRegionConnectorMetadata() {
    }

    public static DatadisRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new DatadisRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "ES";
    }

    @Override
    public long coveredMeteringPoints() {
        return 30234170;
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
        return ZONE_ID_SPAIN;
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.copyOf(SUPPORTED_DATA_NEEDS);
    }
}
