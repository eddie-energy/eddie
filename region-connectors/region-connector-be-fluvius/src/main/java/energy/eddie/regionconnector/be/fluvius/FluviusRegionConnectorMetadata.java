package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S6548") // False positive
@Component
public class FluviusRegionConnectorMetadata implements RegionConnectorMetadata {

    public static final String REGION_CONNECTOR_ID = "be-fluvius";
    public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(
            ValidatedHistoricalDataDataNeed.class
    );
    private static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(
            Granularity.PT15M, // Electricity
            Granularity.PT30M, // Gas
            Granularity.P1D // Both
    );
    private static final List<Granularity> SANDBOX_SUPPORTED_GRANULARITIES = List.of(
            Granularity.PT15M,
            Granularity.P1D
    );
    private static final Map<EnergyType, List<Granularity>> GRANULARITIES_FOR_ENERGY_TYPE = Map.of(
            EnergyType.ELECTRICITY, List.of(Granularity.PT15M, Granularity.P1D),
            EnergyType.NATURAL_GAS, List.of(Granularity.PT30M, Granularity.P1D)
    );
    private static final Map<EnergyType, List<Granularity>> SANDBOX_GRANULARITIES_FOR_ENERGY_TYPE = Map.of(
            EnergyType.ELECTRICITY, List.of(Granularity.PT15M, Granularity.P1D),
            EnergyType.NATURAL_GAS, List.of(Granularity.PT15M, Granularity.P1D)
    );
    private static final FluviusRegionConnectorMetadata INSTANCE = new FluviusRegionConnectorMetadata();
    private final boolean fluviusSandboxEnabled;

    public FluviusRegionConnectorMetadata() {
        this(false);
    }

    @Autowired
    public FluviusRegionConnectorMetadata(@Value("${region-connector.be.fluvius.mock-mandates:false}") boolean fluviusSandboxEnabled) {
        this.fluviusSandboxEnabled = fluviusSandboxEnabled;
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
        return fluviusSandboxEnabled ? SANDBOX_SUPPORTED_GRANULARITIES : SUPPORTED_GRANULARITIES;
    }

    @Override
    public ZoneId timeZone() {
        return ZoneId.of("Europe/Brussels");
    }

    @Override
    public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
        return List.copyOf(SUPPORTED_DATA_NEEDS);
    }

    @Override
    public List<EnergyType> supportedEnergyTypes() {
        return List.of(EnergyType.ELECTRICITY, EnergyType.NATURAL_GAS);
    }

    @Override
    public List<Granularity> granularitiesFor(EnergyType energyType) {
        var supportedGranularities = fluviusSandboxEnabled
                ? SANDBOX_GRANULARITIES_FOR_ENERGY_TYPE
                : GRANULARITIES_FOR_ENERGY_TYPE;
        return supportedGranularities.getOrDefault(energyType, List.of());
    }
}
