package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.shared.utils.DataNeedUtils;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;

@SuppressWarnings("java:S6548") // False positive
public class FluviusRegionConnectorMetadata implements RegionConnectorMetadata {

    public static final String REGION_CONNECTOR_ID = "be-fluvius";
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(
            ValidatedHistoricalDataDataNeed.class
    );
    private static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(
            Granularity.PT15M, // Electricity
            Granularity.PT30M // Gas
    );
    private static final FluviusRegionConnectorMetadata INSTANCE = new FluviusRegionConnectorMetadata();

    private FluviusRegionConnectorMetadata() {}

    public static RegionConnectorMetadata getInstance() {
        return INSTANCE;
    }

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
    public List<Granularity> supportedGranularities() {
        return SUPPORTED_GRANULARITIES;
    }

    @Override
    public ZoneId timeZone() {
        return ZoneId.of("Europe/Brussels");
    }

    @Override
    public List<String> supportedDataNeeds() {
        return DataNeedUtils.convertDataNeedClassesToString(SUPPORTED_DATA_NEEDS);
    }
}
