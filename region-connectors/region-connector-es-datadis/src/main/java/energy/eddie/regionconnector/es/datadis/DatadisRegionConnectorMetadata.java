package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.ZoneId;
import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static energy.eddie.api.agnostic.Granularity.PT1H;

public class DatadisRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "es-datadis";
    public static final String BASE_PATH = "/region-connectors/" + REGION_CONNECTOR_ID;
    public static final ZoneId ZONE_ID_SPAIN = ZoneId.of("Europe/Madrid");
    /**
     * Datadis gives access to metering data for a maximum of 24 months in the past.
     */
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 24;
    /**
     * Datadis can grant permissions for a maximum of 24 months in the future.
     * If no end date is provided, the end date will be set to the current date plus 24 months.
     */
    public static final int MAXIMUM_MONTHS_IN_THE_FUTURE = 24;
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(PT15M, PT1H);
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
}