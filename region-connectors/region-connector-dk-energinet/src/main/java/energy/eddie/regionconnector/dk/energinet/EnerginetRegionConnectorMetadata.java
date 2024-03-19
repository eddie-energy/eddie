package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;

public class EnerginetRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "dk-energinet";
    public static final ZoneId DK_ZONE_ID = ZoneId.of("Europe/Copenhagen");
    public static final Period PERIOD_EARLIEST_START = Period.ofYears(-2);
    public static final Period PERIOD_LATEST_END = Period.ofYears(2);
    public static final long MAXIMUM_MONTHS_IN_THE_PAST = Math.abs(PERIOD_EARLIEST_START.toTotalMonths());
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
}
